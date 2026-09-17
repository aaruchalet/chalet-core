{{/*
Expand the name of the chart.
*/}}
{{- define "chalet-core.name" -}}
{{- default .Chart.Name .Values.nameOverride | trunc 63 | trimSuffix "-" }}
{{- end }}

{{/*
Create a default fully qualified app name.
We truncate at 63 chars because some Kubernetes name fields are limited to this (by the DNS naming spec).
If release name contains chart name it will be used as a full name.
*/}}
{{- define "chalet-core.fullname" -}}
{{- if .Values.fullnameOverride }}
{{- .Values.fullnameOverride | trunc 63 | trimSuffix "-" }}
{{- else }}
{{- $name := default .Chart.Name .Values.nameOverride }}
{{- if contains $name .Release.Name }}
{{- .Release.Name | trunc 63 | trimSuffix "-" }}
{{- else }}
{{- printf "%s-%s" .Release.Name $name | trunc 63 | trimSuffix "-" }}
{{- end }}
{{- end }}
{{- end }}

{{/*
Create chart name and version as used by the chart label.
*/}}
{{- define "chalet-core.chart" -}}
{{- printf "%s-%s" .Chart.Name .Chart.Version | replace "+" "_" | trunc 63 | trimSuffix "-" }}
{{- end }}

{{/*
Common labels
*/}}
{{- define "chalet-core.labels" -}}
helm.sh/chart: {{ include "chalet-core.chart" . }}
{{ include "chalet-core.selectorLabels" . }}
{{- if .Chart.AppVersion }}
app.kubernetes.io/version: {{ .Chart.AppVersion | quote }}
{{- end }}
app.kubernetes.io/managed-by: {{ .Release.Service }}
{{- end }}

{{/*
Selector labels
*/}}
{{- define "chalet-core.selectorLabels" -}}
app.kubernetes.io/name: {{ include "chalet-core.name" . }}
app.kubernetes.io/instance: {{ .Release.Name }}
{{- end }}

{{/*
Create the name of the service account to use
*/}}
{{- define "chalet-core.serviceAccountName" -}}
{{- if .Values.serviceAccount.create }}
{{- default (include "chalet-core.fullname" .) .Values.serviceAccount.name }}
{{- else }}
{{- default "default" .Values.serviceAccount.name }}
{{- end }}
{{- end }}

{{/*
Resolve the MySQL root credentials secret name. Users may provide an existing Secret.
*/}}
{{- define "chalet-core.mysqlSecretName" -}}
{{- default "chalet-core-mysql-secret" .Values.mysql.auth.existingSecret -}}
{{- end }}

{{/*
Resolve the application MySQL credentials secret name. Users may provide an existing Secret.
*/}}
{{- define "chalet-core.mysqlAppSecretName" -}}
{{- default "chalet-core-mysql-app-secret" .Values.mysql.auth.appExistingSecret -}}
{{- end }}
