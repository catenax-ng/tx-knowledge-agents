# Copyright (c) 2022,2024 Contributors to the Eclipse Foundation
#
# See the NOTICE file(s) distributed with this work for additional
# information regarding copyright ownership.
#
# This program and the accompanying materials are made available under the
# terms of the Apache License, Version 2.0 which is available at
# https://www.apache.org/licenses/LICENSE-2.0.
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
# WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
# License for the specific language governing permissions and limitations
# under the License.
#
# SPDX-License-Identifier: Apache-2.0
{{/*
Expand the name of the chart.
*/}}
{{- define "agent.name" -}}
{{- default .Chart.Name .Values.nameOverride | trunc 63 | trimSuffix "-" }}
{{- end }}

{{/*
Create a default fully qualified app name.
We truncate at 63 chars because some Kubernetes name fields are limited to this (by the DNS naming spec).
If release name contains chart name it will be used as a full name.
*/}}
{{- define "agent.fullname" -}}
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
Create a default fully qualified app name for the connector.
We truncate at 63 chars because some Kubernetes name fields are limited to this (by the DNS naming spec).
If release name contains chart name it will be used as a full name.
*/}}
{{- define "agent.connector.fullname" -}}
{{- if .Values.connector }}
{{- printf "%s-%s" .Release.Name .Values.connector | trunc 63 | trimSuffix "-" }}
{{- else }}
{{- printf "%s" .Release.Name | trunc 63 | trimSuffix "-" }}
{{- end }}
{{- end }}

{{/*
Create chart name and version as used by the chart label.
*/}}
{{- define "agent.chart" -}}
{{- printf "%s-%s" .Chart.Name .Chart.Version | replace "+" "_" | trunc 63 | trimSuffix "-" }}
{{- end }}

{{/*
Common labels
*/}}
{{- define "agent.labels" -}}
helm.sh/chart: {{ include "agent.chart" . }}
{{ include "agent.selectorLabels" . }}
{{ include "agent.customLabels" . }}
{{- if .Chart.AppVersion }}
app.kubernetes.io/version: {{ .Chart.AppVersion | quote }}
{{- end }}
app.kubernetes.io/managed-by: {{ .Release.Service }}
{{- end }}

{{/*
Selector labels
*/}}
{{- define "agent.selectorLabels" -}}
app.kubernetes.io/name: {{ include "agent.name" . }}
app.kubernetes.io/instance: {{ .Release.Name }}
{{- end }}

{{/*
Custom labels
*/}}
{{- define "agent.customLabels" -}}
{{- with .Values.customLabels }}
{{ toYaml . }}
{{- end }}
{{- end }}

{{/*
Create the name of the service account to use
*/}}
{{- define "agent.serviceAccountName" -}}
{{- if .Values.serviceAccount.create }}
{{- default (include "agent.fullname" .) .Values.serviceAccount.name }}
{{- else }}
{{- default "default" .Values.serviceAccount.name }}
{{- end }}
{{- end }}

{{/*
Control DSP URL
*/}}
{{- define "agent.controlplane.url.protocol" -}}
{{- if (and .Values.controlplane.url .Values.controlplane.url.protocol) }}{{/* if dsp api url has been specified explicitly */}}
{{- .Values.controlplane.url.protocol }}
{{- else }}{{/* else when dsp api url has not been specified explicitly */}}
{{- with (index .Values.controlplane.ingresses 0) }}
{{- if .enabled }}{{/* if ingress enabled */}}
{{- if .tls.enabled }}{{/* if TLS enabled */}}
{{- printf "https://%s" .hostname -}}
{{- else }}{{/* else when TLS not enabled */}}
{{- printf "http://%s" .hostname -}}
{{- end }}{{/* end if tls */}}
{{- else }}{{/* else when ingress not enabled */}}
{{- printf "http://%s-controlplane:%v" ( include "agent.connector.fullname" $ ) $.Values.controlplane.endpoints.protocol.port -}}
{{- end }}{{/* end if ingress */}}
{{- end }}{{/* end with ingress */}}
{{- end }}{{/* end if .Values.controlplane.url.protocol */}}
{{- end }}

{{/*
Validation URL
*/}}
{{- define "agent.controlplane.url.control" -}}
{{- printf "http://%s-controlplane:%v%s" ( include "agent.connector.fullname" $ ) .Values.controlplane.endpoints.control.port .Values.controlplane.endpoints.control.path -}}
{{- end }}

{{/*
Validation URL
*/}}
{{- define "agent.controlplane.url.management" -}}
{{- printf "http://%s-controlplane:%v%s" ( include "agent.connector.fullname" $ ) .Values.controlplane.endpoints.management.port .Values.controlplane.endpoints.management.path -}}
{{- end }}

{{/*
Data Control URL (Expects the Chart Root to be accessible via .root, the current dataplane via .dataplane)
*/}}
{{- define "agent.url.callback" -}}
{{- printf "http://%s:%v%s" (include "agent.fullname" . ) .Values.agent.endpoints.callback.port .Values.agent.endpoints.callback.path -}}
{{- end }}

{{/*
join a map
*/}}
{{- define "agent.remotes" -}}
{{- $res := dict "servers" (list) -}}
{{- range $bpn, $connector := .Values.agent.connectors -}}
{{- $noop := printf "%s=%s" $bpn $connector | append $res.servers | set $res "servers" -}}
{{- end -}}
{{- join "," $res.servers }}
{{- end -}}
