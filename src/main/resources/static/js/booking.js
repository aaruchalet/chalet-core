const state = {
  roomTypes: [],
  selectedRoom: null,
  selectedImage: null,
  lookupMode: "email",
  customerId: null,
  bookingId: null,
  bookingIds: [],
  guestRooms: [{ adults: 2, childAge: null }],
  wishlist: [],
  accountBookings: [],
  holdSeconds: 600,
  holdInterval: null,
  authUser: null,
  googleEnabled: false,
  authMode: "signin",
  otpIdentifier: null,
};

const fallbackRooms = [
  { id: 1, typeName: "Deluxe Chalet", pricePerNight: 6500, type: "chalet", image: "https://images.unsplash.com/photo-1600607687939-ce8a6c25118c?auto=format&fit=crop&w=1200&q=85", description: "Spacious and comfortable chalet with stunning mountain views, warm timber interiors and a private-feel retreat.", facts: ["2 Guests", "1 King Bed", "250 sq ft"], amenities: ["Free Wi-Fi", "Breakfast", "Mountain View", "Heater"], popular: true },
  { id: 2, typeName: "Premium Chalet", pricePerNight: 8900, type: "chalet", image: "https://images.unsplash.com/photo-1600566753086-00f18fb6b3ea?auto=format&fit=crop&w=1200&q=85", description: "A premium mountain stay with extra space, better views and enhanced comfort for a relaxing escape.", facts: ["2 Guests", "1 King Bed", "300 sq ft"], amenities: ["Free Wi-Fi", "Breakfast", "Balcony", "Heater"] },
  { id: 3, typeName: "Family Suite", pricePerNight: 14500, type: "suite", image: "https://images.unsplash.com/photo-1615874959474-d609969a20ed?auto=format&fit=crop&w=1200&q=85", description: "Ideal for families with a separate living area, beautiful views and generous space for longer stays.", facts: ["4 Guests", "2 Beds", "500 sq ft"], amenities: ["Free Wi-Fi", "Breakfast", "Living Area", "Mountain View"] }
];

const $ = (id) => document.getElementById(id);
const formatMoney = (value) => new Intl.NumberFormat("en-IN", { style: "currency", currency: "INR", maximumFractionDigits: 0 }).format(Number(value || 0));
const today = new Date();
const pad = (n) => String(n).padStart(2, "0");
const dateString = (d) => `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())}`;

function defaultDates() {
  const inDate = new Date(today);
  inDate.setDate(inDate.getDate() + 3);
  const outDate = new Date(inDate);
  outDate.setDate(outDate.getDate() + 3);
  $("checkIn").min = dateString(today);
  $("checkOut").min = dateString(today);
  $("checkIn").value = dateString(inDate);
  $("checkOut").value = dateString(outDate);
}

async function api(url, options = {}) {
  const response = await fetch(url, {
    credentials: "same-origin",
    headers: { "Content-Type": "application/json", ...(options.headers || {}) },
    ...options
  });
  const text = await response.text();
  let body = null;
  try { body = text ? JSON.parse(text) : null; } catch { body = text; }
  if (!response.ok) {
    let message;
    if (body?.error && typeof body.error === "string") {
      message = body.error;
    } else if (body?.error && typeof body.error === "object") {
      message = Object.values(body.error).join(" ");
    } else {
      message = body?.message || (typeof body === "string" ? body : `Request failed (${response.status})`);
    }
    throw new Error(message);
  }
  return body;
}


function setAuthStatus(message = "", kind = "error") {
  const status = $("authStatus");
  status.textContent = message;
  status.classList.toggle("success", Boolean(message) && kind === "success");
  status.classList.toggle("error", Boolean(message) && kind !== "success");
}

function setAuthMode(mode) {
  state.authMode = mode;
  document.querySelectorAll("[data-auth-mode]").forEach((button) => {
    button.classList.toggle("active", button.dataset.authMode === mode);
  });
  $("signInPane").hidden = mode !== "signin";
  $("signUpPane").hidden = mode !== "signup";
  $("authModalTitle").textContent = mode === "signin" ? "Welcome back" : "Create your account";
  $("authModalIntro").textContent = mode === "signin"
    ? "Sign in as an existing guest or member using your password or a one-time OTP."
    : "Join Aaru’s Chalet with your basic details and receive 500 welcome points worth ₹500.";
  setAuthStatus("");
}

function renderAuthState() {
  const user = state.authUser;
  $("authSignedOut").hidden = Boolean(user);
  $("authSignedIn").hidden = !user;

  if (!user) {
    $("signInLabel").textContent = "Sign in";
    $("signInButton").classList.remove("authenticated");
    $("accountMenu").hidden = true;
    return;
  }

  const firstName = (user.name || "Member").trim().split(/\s+/)[0];
  $("signInLabel").textContent = `Hi, ${firstName}`;
  $("signInButton").classList.add("authenticated");
  $("accountWelcome").textContent = `Welcome back, ${firstName}. Manage your Chalet account here.`;
  $("memberAvatar").textContent = firstName.charAt(0).toUpperCase() || "A";
  $("signedInName").textContent = user.name || "Aaru’s Chalet member";
  $("signedInDetails").textContent = [user.email, user.phone, user.location]
    .filter(Boolean)
    .join(" • ");
  const points = Number(user.rewardPoints || 0);
  $("memberPoints").hidden = points <= 0;
  $("memberPoints").textContent = points > 0
    ? `${points} membership points · ₹${points} booking value`
    : "";
}

function setAuthUser(user) {
  state.authUser = user || null;
  state.customerId = user?.customerId || null;
  renderAuthState();
  renderProfile();
  renderWishlist();
  if (!user) {
    $("accountMenu").hidden = true;
    $("signInButton").setAttribute("aria-expanded", "false");
  }
}

function openAuthModal(mode = "signin") {
  if (!state.authUser) setAuthMode(mode);
  renderAuthState();
  $("authModal").hidden = false;
  document.body.style.overflow = "hidden";
}

function closeAuthModal() {
  $("authModal").hidden = true;
  document.body.style.overflow = "";
}

async function loadAuthConfig() {
  state.googleEnabled = false;
  $("googleAuthButton").disabled = true;
  $("googleConfigNote").hidden = false;
}

async function loadAuthState() {
  try {
    const response = await api("/api/v1/auth/me");
    setAuthUser(response?.data || null);
  } catch {
    setAuthUser(null);
  }
}

async function signInMember(event) {
  event.preventDefault();
  setAuthStatus("Signing in…", "success");
  try {
    const response = await api("/api/v1/auth/signin", {
      method: "POST",
      body: JSON.stringify({
        identifier: $("signInIdentifier").value.trim(),
        password: $("signInPassword").value
      })
    });
    setAuthUser(response?.data);
    setAuthStatus("");
    closeAuthModal();
    toast("Signed in successfully.");
  } catch (error) {
    setAuthStatus(error.message, "error");
  }
}

async function signUpMember(event) {
  event.preventDefault();
  const phone = $("signUpPhone").value.trim();
  if (!/^\d{10}$/.test(phone)) {
    setAuthStatus("Phone number must be exactly 10 digits.");
    return;
  }

  setAuthStatus("Creating your account…", "success");
  try {
    const response = await api("/api/v1/auth/signup", {
      method: "POST",
      body: JSON.stringify({
        name: $("signUpName").value.trim(),
        email: $("signUpEmail").value.trim(),
        phone,
        password: $("signUpPassword").value,
        location: $("signUpLocation").value.trim()
      })
    });
    setAuthUser(response?.data);
    setAuthStatus("Welcome! 500 points worth ₹500 have been added to your membership.", "success");
    closeAuthModal();
    toast("Welcome to Aaru’s Chalet — ₹500 in welcome points added.");
  } catch (error) {
    setAuthStatus(error.message, "error");
  }
}

async function requestLoginOtp() {
  const identifier = $("signInIdentifier").value.trim();
  if (!identifier) {
    setAuthStatus("Enter your email or phone number first.");
    return;
  }

  setAuthStatus("Checking your Aaru’s Chalet membership…", "success");
  try {
    const response = await api("/api/v1/auth/otp/request", {
      method: "POST",
      body: JSON.stringify({ identifier })
    });
    state.otpIdentifier = identifier;
    $("otpPanel").hidden = false;

    const challenge = response?.data;
    if (challenge?.devOtp) {
      $("otpCode").value = challenge.devOtp;
      $("otpStatus").textContent =
        `Local development OTP: ${challenge.devOtp} · expires in ${challenge.expiresInSeconds} seconds.`;
    } else {
      $("otpStatus").textContent =
        `OTP created for ${challenge?.maskedDestination || "your account"}. Enter the 6-digit code.`;
    }
    setAuthStatus("OTP is ready. Enter the 6-digit code below.", "success");
  } catch (error) {
    $("otpPanel").hidden = true;
    state.otpIdentifier = null;
    setAuthStatus(error.message, "error");
  }
}

async function verifyLoginOtp() {
  const code = $("otpCode").value.trim();
  if (!/^\d{6}$/.test(code)) {
    setAuthStatus("Enter the 6-digit OTP.");
    return;
  }

  setAuthStatus("Verifying OTP…", "success");
  try {
    const response = await api("/api/v1/auth/otp/verify", {
      method: "POST",
      body: JSON.stringify({
        identifier: state.otpIdentifier || $("signInIdentifier").value.trim(),
        code
      })
    });
    setAuthUser(response?.data);
    $("otpPanel").hidden = true;
    setAuthStatus("");
    closeAuthModal();
    toast("OTP verified. You are signed in.");
  } catch (error) {
    setAuthStatus(error.message, "error");
  }
}

function continueWithGoogle() {
  if (!state.googleEnabled) {
    setAuthStatus("Google sign-in is coming soon. Please use email/phone with password or OTP.", "error");
    return;
  }
  window.location.assign("/oauth2/authorization/google");
}

async function signOutMember() {
  try {
    await api("/api/v1/auth/logout", { method: "POST" });
  } catch {
    // Clear the local UI state even if the session has already expired.
  }
  setAuthUser(null);
  closeAuthModal();
  toast("Signed out.");
}

function prefillGuestFromAuth() {
  const user = state.authUser;
  if (!user) return;
  $("guestName").value = user.name || "";
  $("guestEmail").value = user.email || "";
  $("guestPhone").value = user.phone || "";
  $("guestAddress").value = user.location || "";
  $("guestMember").value = "true";
}

async function initAuthentication() {
  await Promise.all([loadAuthConfig(), loadAuthState()]);

  const params = new URLSearchParams(window.location.search);
  const authResult = params.get("auth");
  if (authResult === "google-success") {
    await loadAuthState();
    toast("Signed in with Google.");
  } else if (authResult === "google-error") {
    toast("Google sign-in could not be completed.");
  }

  if (authResult) {
    params.delete("auth");
    const query = params.toString();
    history.replaceState({}, "", window.location.pathname + (query ? `?${query}` : "") + window.location.hash);
  }
}

async function loadRoomTypes() {
  try {
    const response = await api("/api/v1/room-types");
    const backendRooms = Array.isArray(response?.data) ? response.data : [];
    state.roomTypes = backendRooms.length ? backendRooms.map((room, index) => ({
      ...fallbackRooms[index % fallbackRooms.length],
      id: room.id,
      typeName: room.typeName,
      pricePerNight: Number(room.pricePerNight)
    })) : fallbackRooms;
  } catch {
    state.roomTypes = fallbackRooms;
    toast("Showing design sample rooms. Start Chalet Core APIs to load live room types.");
  }
  renderRooms();
}

function renderRooms() {
  const maxPrice = Number($("priceRange").value);
  const checkedTypes = [...document.querySelectorAll(".type-filter:checked")].map((el) => el.value);
  let rooms = state.roomTypes.filter((room) => Number(room.pricePerNight) <= maxPrice && checkedTypes.includes(room.type || "chalet"));

  const sort = $("sortSelect").value;
  if (sort === "price-low") rooms.sort((a, b) => a.pricePerNight - b.pricePerNight);
  if (sort === "price-high") rooms.sort((a, b) => b.pricePerNight - a.pricePerNight);

  if (!rooms.length) {
    $("roomList").innerHTML = '<article class="empty-card">No stays match these filters.</article>';
    return;
  }

  $("roomList").innerHTML = rooms.map((room, index) => `
    <article class="stay-card ${state.selectedRoom?.id === room.id ? "selected" : ""}" data-id="${room.id}">
      <div class="stay-image" style="background-image:url('${room.image}')">
        ${room.popular ? '<span class="popular-badge">♛ Most Popular</span>' : ""}
        <button class="save-stay-button ${isSaved(room.id) ? "saved" : ""}" type="button" data-save-room="${room.id}" aria-label="${isSaved(room.id) ? "Remove from saved stays" : "Save stay"}">${isSaved(room.id) ? "♥" : "♡"}</button>
        <span class="image-count">1 / 8</span>
      </div>
      <div class="stay-body">
        <h3>${escapeHtml(room.typeName)}</h3>
        <div class="quick-facts">${room.facts.map((fact) => `<span>◌ ${escapeHtml(fact)}</span>`).join("")}</div>
        <p class="stay-description">${escapeHtml(room.description)}</p>
        <div class="amenity-tags">${room.amenities.map((item) => `<span>${escapeHtml(item)}</span>`).join("")}</div>
      </div>
      <div class="stay-price">
        <strong>${formatMoney(room.pricePerNight)}</strong>
        <small>per night</small>
        <button class="select-stay-button ${state.selectedRoom?.id === room.id ? "selected" : ""}" type="button" data-select-room="${room.id}">
          ${state.selectedRoom?.id === room.id ? "Selected" : "View Details"}
        </button>
      </div>
    </article>
  `).join("");

  document.querySelectorAll("[data-select-room]").forEach((button) => {
    button.addEventListener("click", () => selectRoom(Number(button.dataset.selectRoom)));
  });
  document.querySelectorAll("[data-save-room]").forEach((button) => {
    button.addEventListener("click", () => toggleWishlist(Number(button.dataset.saveRoom)));
  });
}

function selectRoom(id) {
  state.selectedRoom = state.roomTypes.find((room) => room.id === id) || null;
  state.selectedImage = state.selectedRoom?.image || null;
  renderRooms();
  updateSummary();
}

function numberOfNights() {
  const checkIn = $("checkIn").value;
  const checkOut = $("checkOut").value;
  if (!checkIn || !checkOut) return 0;
  const start = new Date(checkIn + "T00:00:00");
  const end = new Date(checkOut + "T00:00:00");
  return Math.round((end - start) / 86400000);
}

function prettyDate(value) {
  if (!value) return "";
  return new Date(value + "T00:00:00").toLocaleDateString("en-GB", { day: "2-digit", month: "short", year: "numeric" });
}

function guestSummary() {
  let adults = 0;
  let children = 0;
  state.guestRooms.forEach((entry) => {
    adults += Number(entry.adults || 0);
    if (entry.childAge !== null && entry.childAge !== "") {
      if (Number(entry.childAge) >= 12) adults += 1;
      else children += 1;
    }
  });
  const rooms = state.guestRooms.length;
  return `${adults} Adult${adults === 1 ? "" : "s"}${children ? ` · ${children} Child${children === 1 ? "" : "ren"}` : ""} · ${rooms} Room${rooms === 1 ? "" : "s"}`;
}

function childSupplementPerNight() {
  if (!state.selectedRoom) return 0;
  const eligibleChildren = state.guestRooms.filter((entry) => {
    const age = Number(entry.childAge);
    return entry.childAge !== null && entry.childAge !== "" && age >= 2 && age < 12;
  }).length;
  return eligibleChildren * Number(state.selectedRoom.pricePerNight) * 0.5;
}

function validateOccupancy() {
  state.guestRooms.forEach((entry, index) => {
    const adults = Number(entry.adults || 0);
    const hasChild = entry.childAge !== null && entry.childAge !== "";
    const childAge = hasChild ? Number(entry.childAge) : null;
    const effectiveAdults = adults + (childAge !== null && childAge >= 12 ? 1 : 0);
    const youngChildren = childAge !== null && childAge < 12 ? 1 : 0;
    if (effectiveAdults < 1) throw new Error(`Room ${index + 1} needs at least one adult.`);
    if (effectiveAdults > 3 || effectiveAdults + youngChildren > 3) {
      throw new Error(`Room ${index + 1} exceeds the maximum occupancy of 3. Add another room.`);
    }
    if (youngChildren && adults > 2) {
      throw new Error(`Room ${index + 1} can have at most 2 adults when a child is included.`);
    }
  });
}

function updateSummary() {
  const nights = numberOfNights();
  const room = state.selectedRoom;
  const guestText = guestSummary();
  $("summaryGuests").textContent = guestText;
  $("guestSelectorSummary").textContent = guestText;

  if ($("checkIn").value && $("checkOut").value) {
    $("summaryDates").textContent = `${prettyDate($("checkIn").value)} – ${prettyDate($("checkOut").value)}`;
    $("summaryNights").textContent = nights > 0 ? `${nights} night${nights === 1 ? "" : "s"}` : "Invalid dates";
    $("availabilitySummary").textContent = `${prettyDate($("checkIn").value)} – ${prettyDate($("checkOut").value)} • ${guestText}`;
  }

  if (!room) {
    $("summaryRoomName").textContent = "Choose a stay";
    $("summaryRate").textContent = "—";
    $("roomAmount").textContent = "—";
    $("childSupplementRow").hidden = true;
    $("taxAmount").textContent = "—";
    $("totalAmount").textContent = "—";
    $("continueButton").disabled = true;
    return;
  }

  $("summaryRoomName").textContent = room.typeName;
  $("summaryRate").textContent = formatMoney(room.pricePerNight);
  $("summaryImage").style.backgroundImage = `url('${room.image}')`;

  const roomCount = state.guestRooms.length;
  const baseAmount = nights > 0 ? nights * Number(room.pricePerNight) * roomCount : 0;
  const childAmount = nights > 0 ? nights * childSupplementPerNight() : 0;
  const subtotal = baseAmount + childAmount;
  const taxes = Math.round(subtotal * 0.12);
  $("roomAmount").textContent = baseAmount ? formatMoney(baseAmount) : "—";
  $("childSupplementRow").hidden = childAmount <= 0;
  $("childSupplementAmount").textContent = childAmount ? formatMoney(childAmount) : "—";
  $("taxAmount").textContent = subtotal ? formatMoney(taxes) : "—";
  $("totalAmount").textContent = subtotal ? formatMoney(subtotal + taxes) : "—";
  $("continueButton").disabled = !(room && nights > 0);
}

function validateStay() {
  const nights = numberOfNights();
  if (!$("checkIn").value || !$("checkOut").value) throw new Error("Choose check-in and check-out dates.");
  if (nights <= 0) throw new Error("Check-out must be after check-in.");
  if (!state.selectedRoom) throw new Error("Choose a room type.");
  validateOccupancy();
}

async function lookupGuest() {
  const value = $("lookupValue").value.trim();
  if (!value) return toast("Enter an email or phone number.");
  const url = state.lookupMode === "email"
    ? `/api/v1/customer/by-email?email=${encodeURIComponent(value)}`
    : `/api/v1/customer/by-phone?phone=${encodeURIComponent(value)}`;
  $("lookupStatus").textContent = "Searching…";

  try {
    const response = await api(url);
    const guest = response?.data;
    if (!guest?.id) throw new Error("Guest not found");
    state.customerId = guest.id;
    $("guestName").value = guest.name || "";
    $("guestEmail").value = guest.email || "";
    $("guestPhone").value = guest.phone || "";
    $("guestAddress").value = guest.address || "";
    $("guestMember").value = String(Boolean(guest.member));
    $("lookupStatus").textContent = `Found ${guest.name}. You can hold the room now.`;
    toast("Existing guest loaded.");
  } catch (error) {
    state.customerId = null;
    $("lookupStatus").textContent = "No existing guest found. Create a new guest below.";
  }
}

async function linkCurrentMemberCustomer(customerId) {
  if (!state.authUser || state.authUser.customerId) return;
  const response = await api(`/api/v1/auth/customer/${customerId}`, { method: "POST" });
  setAuthUser(response?.data);
}

async function createGuest() {
  const phone = $("guestPhone").value.trim();
  if (!/^\d{10}$/.test(phone)) throw new Error("Phone number must be exactly 10 digits.");
  const payload = {
    name: $("guestName").value.trim(),
    email: $("guestEmail").value.trim(),
    phone,
    address: $("guestAddress").value.trim(),
    member: $("guestMember").value === "true"
  };
  const response = await api("/api/v1/customer", {
    method: "POST",
    body: JSON.stringify(payload)
  });
  if (!response?.data?.id) throw new Error("Customer creation did not return an ID.");
  await linkCurrentMemberCustomer(response.data.id);
  return response.data.id;
}

async function createHold(customerId) {
  validateStay();
  const payload = {
    customerId,
    roomTypeId: state.selectedRoom.id,
    checkInDate: $("checkIn").value,
    checkOutDate: $("checkOut").value
  };

  const created = [];
  try {
    for (let i = 0; i < state.guestRooms.length; i += 1) {
      const booking = await api("/api/v1/bookings", {
        method: "POST",
        body: JSON.stringify(payload)
      });
      if (!booking?.id) throw new Error("Booking was created but no booking ID was returned.");
      created.push(booking.id);
    }
  } catch (error) {
    await Promise.all(created.map((id) => api(`/api/v1/bookings/${id}/cancel`, { method: "POST" }).catch(() => null)));
    throw error;
  }

  state.bookingIds = created;
  state.bookingId = created[0] || null;
  showHold();
}

function showHold() {
  state.holdSeconds = 600;
  $("holdPanel").hidden = false;
  $("continueButton").hidden = true;
  clearInterval(state.holdInterval);
  tickHold();
  state.holdInterval = setInterval(tickHold, 1000);
  $("guestModal").hidden = true;
  document.body.style.overflow = "";
  toast("Room held for 10 minutes.");
}

function tickHold() {
  const minutes = Math.floor(state.holdSeconds / 60);
  const seconds = state.holdSeconds % 60;
  $("holdTimer").textContent = `${pad(minutes)}:${pad(seconds)}`;
  if (state.holdSeconds <= 0) {
    clearInterval(state.holdInterval);
    $("holdTimer").textContent = "EXPIRED";
    $("confirmButton").disabled = true;
    toast("The booking hold has expired.");
    return;
  }
  state.holdSeconds -= 1;
}

async function confirmBooking() {
  if (!state.bookingIds.length && !state.bookingId) return;
  try {
    const ids = state.bookingIds.length ? state.bookingIds : [state.bookingId];
    await Promise.all(ids.map((id) => api(`/api/v1/bookings/${id}/confirm`, { method: "POST" })));
    clearInterval(state.holdInterval);
    $("holdTimer").textContent = "CONFIRMED";
    $("confirmButton").hidden = true;
    $("cancelButton").hidden = true;
    toast(`${ids.length} room${ids.length === 1 ? "" : "s"} confirmed successfully.`);
    loadMemberBookings();
  } catch (error) {
    toast(error.message);
  }
}

async function cancelBooking() {
  if (!state.bookingIds.length && !state.bookingId) return;
  try {
    const ids = state.bookingIds.length ? state.bookingIds : [state.bookingId];
    await Promise.all(ids.map((id) => api(`/api/v1/bookings/${id}/cancel`, { method: "POST" })));
    resetHold();
    toast("Booking hold cancelled.");
  } catch (error) {
    toast(error.message);
  }
}

function resetHold() {
  clearInterval(state.holdInterval);
  state.bookingId = null;
  state.bookingIds = [];
  $("holdPanel").hidden = true;
  $("continueButton").hidden = false;
  $("confirmButton").hidden = false;
  $("confirmButton").disabled = false;
  $("cancelButton").hidden = false;
}

function toast(message) {
  const node = $("toast");
  node.textContent = message;
  node.classList.add("show");
  clearTimeout(window.__toastTimer);
  window.__toastTimer = setTimeout(() => node.classList.remove("show"), 3200);
}

function escapeHtml(value) {
  return String(value ?? "").replace(/[&<>"']/g, (char) => ({ "&": "&amp;", "<": "&lt;", ">": "&gt;", '"': "&quot;", "'": "&#039;" }[char]));
}


function renderGuestRooms() {
  $("guestRoomList").innerHTML = state.guestRooms.map((entry, index) => `
    <div class="guest-room-card" data-room-index="${index}">
      <div class="guest-room-title"><strong>Room ${index + 1}</strong><small>Up to 3 occupants</small></div>
      <label>Adults
        <select class="room-adults" data-room-index="${index}">
          ${[1,2,3].map((n) => `<option value="${n}" ${Number(entry.adults) === n ? "selected" : ""}>${n} Adult${n === 1 ? "" : "s"}</option>`).join("")}
        </select>
      </label>
      <label>Child
        <select class="room-child-age" data-room-index="${index}">
          <option value="" ${entry.childAge === null || entry.childAge === "" ? "selected" : ""}>No child</option>
          ${Array.from({length: 18}, (_, age) => `<option value="${age}" ${Number(entry.childAge) === age ? "selected" : ""}>Age ${age}${age < 2 ? " · Free" : age < 12 ? " · 50% extra bed" : " · Counts as adult"}</option>`).join("")}
        </select>
      </label>
      <p class="room-rule-message"></p>
    </div>
  `).join("");

  document.querySelectorAll(".room-adults").forEach((select) => {
    select.addEventListener("change", () => {
      state.guestRooms[Number(select.dataset.roomIndex)].adults = Number(select.value);
      validateGuestRoomCard(Number(select.dataset.roomIndex));
      updateSummary();
    });
  });
  document.querySelectorAll(".room-child-age").forEach((select) => {
    select.addEventListener("change", () => {
      state.guestRooms[Number(select.dataset.roomIndex)].childAge = select.value === "" ? null : Number(select.value);
      validateGuestRoomCard(Number(select.dataset.roomIndex));
      updateSummary();
    });
  });
  state.guestRooms.forEach((_, index) => validateGuestRoomCard(index));
}

function validateGuestRoomCard(index) {
  const entry = state.guestRooms[index];
  const card = document.querySelector(`[data-room-index="${index}"].guest-room-card`);
  if (!card) return;
  const message = card.querySelector(".room-rule-message");
  try {
    const adults = Number(entry.adults || 0);
    const hasChild = entry.childAge !== null && entry.childAge !== "";
    const age = hasChild ? Number(entry.childAge) : null;
    const effectiveAdults = adults + (age !== null && age >= 12 ? 1 : 0);
    const youngChild = age !== null && age < 12;
    if (effectiveAdults > 3 || (youngChild && adults > 2) || effectiveAdults + (youngChild ? 1 : 0) > 3) {
      throw new Error("Occupancy exceeded — add another room.");
    }
    if (age !== null && age < 2) message.textContent = "Child under 2 stays free.";
    else if (age !== null && age < 12) message.textContent = "Child extra bed is charged at 50% of the adult room rate.";
    else if (age !== null) message.textContent = "Age 12+ is counted as an adult.";
    else message.textContent = "Maximum 3 adults, or 2 adults + 1 child.";
    card.classList.remove("invalid");
  } catch (error) {
    message.textContent = error.message;
    card.classList.add("invalid");
  }
}

function setRoomCount(count) {
  const target = Math.max(1, Math.min(4, Number(count)));
  while (state.guestRooms.length < target) state.guestRooms.push({ adults: 2, childAge: null });
  while (state.guestRooms.length > target) state.guestRooms.pop();
  $("roomCount").value = String(target);
  renderGuestRooms();
  updateSummary();
}

function initGuestSelector() {
  renderGuestRooms();
  $("guestSelectorButton").addEventListener("click", (event) => {
    event.stopPropagation();
    const opening = $("guestSelectorPanel").hidden;
    $("guestSelectorPanel").hidden = !opening;
    $("guestSelectorButton").setAttribute("aria-expanded", String(opening));
  });
  $("guestSelectorPanel").addEventListener("click", (event) => event.stopPropagation());
  $("roomCount").addEventListener("change", () => setRoomCount($("roomCount").value));
  $("guestApplyButton").addEventListener("click", () => {
    try {
      validateOccupancy();
      $("guestSelectorPanel").hidden = true;
      $("guestSelectorButton").setAttribute("aria-expanded", "false");
      updateSummary();
    } catch (error) {
      toast(error.message);
    }
  });
}

function wishlistStorageKey() {
  return `aaru-wishlist-${state.authUser?.accountId || "guest"}`;
}

function loadWishlist() {
  try { state.wishlist = JSON.parse(localStorage.getItem(wishlistStorageKey()) || "[]"); }
  catch { state.wishlist = []; }
}

function isSaved(roomId) {
  return state.wishlist.includes(Number(roomId));
}

function toggleWishlist(roomId) {
  const id = Number(roomId);
  if (isSaved(id)) state.wishlist = state.wishlist.filter((value) => value !== id);
  else state.wishlist.push(id);
  localStorage.setItem(wishlistStorageKey(), JSON.stringify(state.wishlist));
  renderRooms();
  renderWishlist();
  toast(isSaved(id) ? "Stay saved to your wishlist." : "Stay removed from your wishlist.");
}

function renderWishlist() {
  if (!$("wishlistList")) return;
  loadWishlist();
  const rooms = state.wishlist.map((id) => state.roomTypes.find((room) => Number(room.id) === Number(id))).filter(Boolean);
  if (!rooms.length) {
    $("wishlistList").innerHTML = '<div class="account-empty">No saved stays yet. Use the heart on any Chalet or Suite to save it.</div>';
    return;
  }
  $("wishlistList").innerHTML = rooms.map((room) => `
    <article class="wishlist-card">
      <div class="wishlist-thumb" style="background-image:url('${room.image}')"></div>
      <div><strong>${escapeHtml(room.typeName)}</strong><small>${formatMoney(room.pricePerNight)} per night</small></div>
      <button type="button" data-remove-wishlist="${room.id}">Remove</button>
    </article>`).join("");
  document.querySelectorAll("[data-remove-wishlist]").forEach((button) => button.addEventListener("click", () => toggleWishlist(Number(button.dataset.removeWishlist))));
}

function renderProfile() {
  if (!$("profileDetails")) return;
  const user = state.authUser;
  if (!user) {
    $("profileDetails").innerHTML = '<div class="account-empty">Sign in to view your profile.</div>';
    return;
  }
  const fields = [
    ["Name", user.name],
    ["Email", user.email],
    ["Phone", user.phone],
    ["Location", user.location],
    ["Membership points", Number(user.rewardPoints || 0).toLocaleString("en-IN")]
  ];
  $("profileDetails").innerHTML = fields.map(([label,value]) => `<div><small>${escapeHtml(label)}</small><strong>${escapeHtml(value || "—")}</strong></div>`).join("");
}

async function loadMemberBookings() {
  if (!state.authUser?.customerId) {
    $("memberBookings").innerHTML = '<div class="account-empty">Your account is not linked to a customer profile yet. Complete your first booking to create the link.</div>';
    renderPayments([]);
    return;
  }
  $("memberBookings").innerHTML = '<div class="account-empty">Loading bookings…</div>';
  try {
    const response = await api(`/api/v1/bookings/customer/${state.authUser.customerId}`);
    const bookings = Array.isArray(response) ? response : (Array.isArray(response?.data) ? response.data : []);
    state.accountBookings = bookings;
    if (!bookings.length) {
      $("memberBookings").innerHTML = '<div class="account-empty">No bookings yet.</div>';
    } else {
      $("memberBookings").innerHTML = bookings.map((booking) => `
        <article class="booking-history-card">
          <div><small>Booking #${booking.id}</small><strong>${escapeHtml(booking.bookingStatus || "UNKNOWN")}</strong></div>
          <div><small>Stay</small><strong>${prettyDate(booking.checkInDate)} – ${prettyDate(booking.checkOutDate)}</strong></div>
          <div><small>Room ID</small><strong>${escapeHtml(booking.roomId)}</strong></div>
        </article>`).join("");
    }
    renderPayments(bookings);
  } catch (error) {
    $("memberBookings").innerHTML = `<div class="account-empty">Could not load bookings: ${escapeHtml(error.message)}</div>`;
    renderPayments([]);
  }
}

function renderPayments(bookings = state.accountBookings) {
  if (!$("memberPayments")) return;
  const confirmed = bookings.filter((booking) => String(booking.bookingStatus).toUpperCase() === "CONFIRMED");
  if (!confirmed.length) {
    $("memberPayments").innerHTML = '<div class="account-empty">No payment records yet. Payment gateway integration is not enabled in this version.</div>';
    return;
  }
  $("memberPayments").innerHTML = confirmed.map((booking) => `
    <article class="payment-card">
      <div><small>Booking #${booking.id}</small><strong>Payment integration pending</strong></div>
      <span>Confirmed booking</span>
    </article>`).join("");
}

function openAccount(view = "profile") {
  if (!state.authUser) {
    openAuthModal("signin");
    return;
  }
  $("accountMenu").hidden = true;
  $("signInButton").setAttribute("aria-expanded", "false");
  document.querySelectorAll("[data-account-tab]").forEach((button) => button.classList.toggle("active", button.dataset.accountTab === view));
  document.querySelectorAll("[data-account-pane]").forEach((pane) => pane.hidden = pane.dataset.accountPane !== view);
  $("accountModal").hidden = false;
  document.body.style.overflow = "hidden";
  renderProfile();
  renderWishlist();
  if (view === "bookings" || view === "payments") loadMemberBookings();
}

function closeAccount() {
  $("accountModal").hidden = true;
  document.body.style.overflow = "";
}

function experienceStorageKey() {
  return "aaru-guest-experiences";
}

function loadExperiences() {
  let items = [];
  try { items = JSON.parse(localStorage.getItem(experienceStorageKey()) || "[]"); } catch {}
  const empty = $("experienceEmpty");
  document.querySelectorAll(".experience-user-card").forEach((node) => node.remove());
  empty.hidden = items.length > 0;
  items.forEach((item) => {
    const card = document.createElement("article");
    card.className = "experience-user-card";
    card.innerHTML = `<img src="${item.photo}" alt="Guest-shared experience"><div><strong>${escapeHtml(item.name || "Aaru’s Chalet guest")}</strong><p>${escapeHtml(item.comment)}</p></div>`;
    $("experienceGrid").appendChild(card);
  });
}

function openExperienceModal() {
  if (!state.authUser) {
    openAuthModal("signin");
    toast("Sign in before sharing an experience.");
    return;
  }
  $("experienceModal").hidden = false;
  document.body.style.overflow = "hidden";
}

function closeExperienceModal() {
  $("experienceModal").hidden = true;
  document.body.style.overflow = "";
}

function saveExperience(event) {
  event.preventDefault();
  const file = $("experiencePhoto").files[0];
  const comment = $("experienceComment").value.trim();
  if (!file || !comment) return;
  if (file.size > 1500000) {
    toast("Please choose an image smaller than 1.5 MB for this browser-stored version.");
    return;
  }
  const reader = new FileReader();
  reader.onload = () => {
    let items = [];
    try { items = JSON.parse(localStorage.getItem(experienceStorageKey()) || "[]"); } catch {}
    items.unshift({ photo: reader.result, comment, name: state.authUser?.name || "Guest", createdAt: new Date().toISOString() });
    try {
      localStorage.setItem(experienceStorageKey(), JSON.stringify(items.slice(0, 8)));
      $("experienceForm").reset();
      closeExperienceModal();
      loadExperiences();
      toast("Your experience has been added.");
    } catch {
      toast("This photo is too large to store in the browser. Choose a smaller image.");
    }
  };
  reader.readAsDataURL(file);
}


$("signInButton").addEventListener("click", (event) => {
  event.stopPropagation();
  if (!state.authUser) {
    openAuthModal("signin");
    return;
  }
  const opening = $("accountMenu").hidden;
  $("accountMenu").hidden = !opening;
  $("signInButton").setAttribute("aria-expanded", String(opening));
});
$("authModalClose").addEventListener("click", closeAuthModal);
$("authDoneButton").addEventListener("click", closeAuthModal);
$("authModal").addEventListener("click", (event) => {
  if (event.target === $("authModal")) closeAuthModal();
});
document.querySelectorAll("[data-auth-mode]").forEach((button) => {
  button.addEventListener("click", () => setAuthMode(button.dataset.authMode));
});
$("openSignUp").addEventListener("click", () => setAuthMode("signup"));
$("openSignIn").addEventListener("click", () => setAuthMode("signin"));
$("googleAuthButton").addEventListener("click", continueWithGoogle);
$("signInForm").addEventListener("submit", signInMember);
$("signUpForm").addEventListener("submit", signUpMember);
$("otpRequestButton").addEventListener("click", requestLoginOtp);
$("otpVerifyButton").addEventListener("click", verifyLoginOtp);
$("logoutButton").addEventListener("click", signOutMember);
$("headerLogoutButton").addEventListener("click", signOutMember);
document.addEventListener("click", () => {
  $("accountMenu").hidden = true;
  $("signInButton").setAttribute("aria-expanded", "false");
  $("guestSelectorPanel").hidden = true;
  $("guestSelectorButton").setAttribute("aria-expanded", "false");
});
$("accountMenu").addEventListener("click", (event) => event.stopPropagation());
document.querySelectorAll("[data-account-view]").forEach((button) => button.addEventListener("click", () => openAccount(button.dataset.accountView)));
$("savedStaysButton").addEventListener("click", () => openAccount("wishlist"));
document.querySelectorAll("[data-account-tab]").forEach((button) => button.addEventListener("click", () => openAccount(button.dataset.accountTab)));
$("accountModalClose").addEventListener("click", closeAccount);
$("accountModal").addEventListener("click", (event) => { if (event.target === $("accountModal")) closeAccount(); });
$("refreshBookingsButton").addEventListener("click", loadMemberBookings);
$("shareExperienceButton").addEventListener("click", openExperienceModal);
$("experienceModalClose").addEventListener("click", closeExperienceModal);
$("experienceModal").addEventListener("click", (event) => { if (event.target === $("experienceModal")) closeExperienceModal(); });
$("experienceForm").addEventListener("submit", saveExperience);
initGuestSelector();
loadExperiences();

$("searchForm").addEventListener("submit", (event) => {
  event.preventDefault();
  try {
    validateStayDatesOnly();
    updateSummary();
    document.querySelector("#stays").scrollIntoView({ behavior: "smooth", block: "start" });
  } catch (error) {
    toast(error.message);
  }
});

function validateStayDatesOnly() {
  if (!$("checkIn").value || !$("checkOut").value) throw new Error("Select check-in and check-out dates.");
  if (numberOfNights() <= 0) throw new Error("Check-out must be after check-in.");
}

["checkIn", "checkOut"].forEach((id) => $(id).addEventListener("change", () => {
  if (id === "checkIn") $("checkOut").min = $("checkIn").value;
  updateSummary();
}));

$("priceRange").addEventListener("input", () => {
  $("priceRangeValue").textContent = Number($("priceRange").value) >= 50000 ? "₹50,000+" : formatMoney($("priceRange").value);
  renderRooms();
});
$("sortSelect").addEventListener("change", renderRooms);
document.querySelectorAll(".type-filter").forEach((input) => input.addEventListener("change", renderRooms));

$("clearFilters").addEventListener("click", () => {
  $("priceRange").value = 50000;
  $("priceRangeValue").textContent = "₹50,000+";
  document.querySelectorAll(".type-filter").forEach((input) => input.checked = true);
  $("sortSelect").value = "recommended";
  renderRooms();
});

$("continueButton").addEventListener("click", async () => {
  try {
    validateStay();

    if (state.authUser?.customerId) {
      await createHold(state.authUser.customerId);
      return;
    }

    prefillGuestFromAuth();
    $("guestModal").hidden = false;
    document.body.style.overflow = "hidden";
  } catch (error) {
    toast(error.message);
  }
});
$("modalClose").addEventListener("click", () => {
  $("guestModal").hidden = true;
  document.body.style.overflow = "";
});
$("guestModal").addEventListener("click", (event) => {
  if (event.target === $("guestModal")) {
    $("guestModal").hidden = true;
    document.body.style.overflow = "";
  }
});

document.querySelectorAll(".lookup-tab").forEach((button) => {
  button.addEventListener("click", () => {
    document.querySelectorAll(".lookup-tab").forEach((tab) => tab.classList.remove("active"));
    button.classList.add("active");
    state.lookupMode = button.dataset.mode;
    $("lookupValue").type = state.lookupMode === "email" ? "email" : "tel";
    $("lookupValue").placeholder = state.lookupMode === "email" ? "guest@example.com" : "9876543210";
    $("lookupStatus").textContent = "";
  });
});
$("lookupButton").addEventListener("click", lookupGuest);

$("guestForm").addEventListener("submit", async (event) => {
  event.preventDefault();
  try {
    const customerId = state.customerId || await createGuest();
    await linkCurrentMemberCustomer(customerId);
    await createHold(customerId);
  } catch (error) {
    toast(error.message);
  }
});

$("confirmButton").addEventListener("click", confirmBooking);
$("cancelButton").addEventListener("click", cancelBooking);

defaultDates();
initAuthentication().then(() => {
  loadWishlist();
  renderWishlist();
});
loadRoomTypes().then(() => {
  if (state.roomTypes.length) selectRoom(state.roomTypes[0].id);
  loadWishlist();
  renderWishlist();
  updateSummary();
});
