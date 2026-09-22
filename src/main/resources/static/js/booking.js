const state = {
  roomTypes: [],
  selectedRoom: null,
  selectedImage: null,
  lookupMode: "email",
  customerId: null,
  bookingId: null,
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


function setAuthStatus(message = "") {
  $("authStatus").textContent = message;
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
    : "Join Aaru’s Chalet with your basic details, or continue with Google.";
  setAuthStatus("");
}

function renderAuthState() {
  const user = state.authUser;
  $("authSignedOut").hidden = Boolean(user);
  $("authSignedIn").hidden = !user;

  if (!user) {
    $("signInLabel").textContent = "Sign in";
    $("signInButton").classList.remove("authenticated");
    return;
  }

  const firstName = (user.name || "Member").trim().split(/\s+/)[0];
  $("signInLabel").textContent = `Hi, ${firstName}`;
  $("signInButton").classList.add("authenticated");
  $("memberAvatar").textContent = firstName.charAt(0).toUpperCase() || "A";
  $("signedInName").textContent = user.name || "Aaru’s Chalet member";
  $("signedInDetails").textContent = [user.email, user.phone, user.location]
    .filter(Boolean)
    .join(" • ");
}

function setAuthUser(user) {
  state.authUser = user || null;
  state.customerId = user?.customerId || null;
  renderAuthState();
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
  try {
    const response = await api("/api/v1/auth/config");
    state.googleEnabled = Boolean(response?.data?.googleEnabled);
  } catch {
    state.googleEnabled = false;
  }
  $("googleAuthButton").disabled = !state.googleEnabled;
  $("googleConfigNote").hidden = state.googleEnabled;
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
  setAuthStatus("Signing in…");
  try {
    const response = await api("/api/v1/auth/signin", {
      method: "POST",
      body: JSON.stringify({
        identifier: $("signInIdentifier").value.trim(),
        password: $("signInPassword").value
      })
    });
    setAuthUser(response?.data);
    closeAuthModal();
    toast("Signed in successfully.");
  } catch (error) {
    setAuthStatus(error.message);
  }
}

async function signUpMember(event) {
  event.preventDefault();
  const phone = $("signUpPhone").value.trim();
  if (!/^\d{10}$/.test(phone)) {
    setAuthStatus("Phone number must be exactly 10 digits.");
    return;
  }

  setAuthStatus("Creating your account…");
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
    closeAuthModal();
    toast("Account created. You are now signed in.");
  } catch (error) {
    setAuthStatus(error.message);
  }
}

async function requestLoginOtp() {
  const identifier = $("signInIdentifier").value.trim();
  if (!identifier) {
    setAuthStatus("Enter your email or phone number first.");
    return;
  }

  setAuthStatus("Creating OTP…");
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
    setAuthStatus("");
  } catch (error) {
    setAuthStatus(error.message);
  }
}

async function verifyLoginOtp() {
  const code = $("otpCode").value.trim();
  if (!/^\d{6}$/.test(code)) {
    setAuthStatus("Enter the 6-digit OTP.");
    return;
  }

  setAuthStatus("Verifying OTP…");
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
    closeAuthModal();
    toast("OTP verified. You are signed in.");
  } catch (error) {
    setAuthStatus(error.message);
  }
}

function continueWithGoogle() {
  if (!state.googleEnabled) {
    toast("Google sign-in is not configured on the server yet.");
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

function updateSummary() {
  const nights = numberOfNights();
  const room = state.selectedRoom;
  const guestText = $("guests").selectedOptions[0]?.textContent || "2 Adults";
  $("summaryGuests").textContent = guestText;

  if ($("checkIn").value && $("checkOut").value) {
    $("summaryDates").textContent = `${prettyDate($("checkIn").value)} – ${prettyDate($("checkOut").value)}`;
    $("summaryNights").textContent = nights > 0 ? `${nights} night${nights === 1 ? "" : "s"}` : "Invalid dates";
    $("availabilitySummary").textContent = `${prettyDate($("checkIn").value)} – ${prettyDate($("checkOut").value)} • ${guestText}`;
  }

  if (!room) {
    $("summaryRoomName").textContent = "Choose a stay";
    $("summaryRate").textContent = "—";
    $("roomAmount").textContent = "—";
    $("taxAmount").textContent = "—";
    $("totalAmount").textContent = "—";
    $("continueButton").disabled = true;
    return;
  }

  $("summaryRoomName").textContent = room.typeName;
  $("summaryRate").textContent = formatMoney(room.pricePerNight);
  $("summaryImage").style.backgroundImage = `url('${room.image}')`;

  const amount = nights > 0 ? nights * Number(room.pricePerNight) : 0;
  const taxes = Math.round(amount * 0.12);
  $("roomAmount").textContent = amount ? formatMoney(amount) : "—";
  $("taxAmount").textContent = amount ? formatMoney(taxes) : "—";
  $("totalAmount").textContent = amount ? formatMoney(amount + taxes) : "—";
  $("continueButton").disabled = !(room && nights > 0);
}

function validateStay() {
  const nights = numberOfNights();
  if (!$("checkIn").value || !$("checkOut").value) throw new Error("Choose check-in and check-out dates.");
  if (nights <= 0) throw new Error("Check-out must be after check-in.");
  if (!state.selectedRoom) throw new Error("Choose a room type.");
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
  const booking = await api("/api/v1/bookings", {
    method: "POST",
    body: JSON.stringify(payload)
  });
  if (!booking?.id) throw new Error("Booking was created but no booking ID was returned.");
  state.bookingId = booking.id;
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
  if (!state.bookingId) return;
  try {
    await api(`/api/v1/bookings/${state.bookingId}/confirm`, { method: "POST" });
    clearInterval(state.holdInterval);
    $("holdTimer").textContent = "CONFIRMED";
    $("confirmButton").hidden = true;
    $("cancelButton").hidden = true;
    toast("Booking confirmed successfully.");
  } catch (error) {
    toast(error.message);
  }
}

async function cancelBooking() {
  if (!state.bookingId) return;
  try {
    await api(`/api/v1/bookings/${state.bookingId}/cancel`, { method: "POST" });
    resetHold();
    toast("Booking hold cancelled.");
  } catch (error) {
    toast(error.message);
  }
}

function resetHold() {
  clearInterval(state.holdInterval);
  state.bookingId = null;
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


$("signInButton").addEventListener("click", () => openAuthModal("signin"));
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

["checkIn", "checkOut", "guests"].forEach((id) => $(id).addEventListener("change", () => {
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
  document.querySelectorAll(".type-filter").forEach((input, index) => input.checked = index === 0);
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
    await createHold(customerId);
  } catch (error) {
    toast(error.message);
  }
});

$("confirmButton").addEventListener("click", confirmBooking);
$("cancelButton").addEventListener("click", cancelBooking);

defaultDates();
initAuthentication();
loadRoomTypes().then(() => {
  if (state.roomTypes.length) selectRoom(state.roomTypes[0].id);
  updateSummary();
});
