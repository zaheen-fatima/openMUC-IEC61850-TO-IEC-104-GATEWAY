# OpenMUC IEC61850 to IEC104 Gateway Application

## Overview
This Java OSGi project reads real-time data from IEC 61850 devices (IEDs) and forwards it to an IEC 104 server using the OpenMUC framework. The application supports automatic channel mapping via `channels.xml` and provides real-time updates to IEC 104 clients.

## Features
- Reads data from IEC 61850 IEDs.
- Forwards data to IEC 104 channels with proper type conversion.
- Configurable via `channels.xml`.
- Fully OSGi-compliant and runs on Apache Felix.

## Prerequisites
- Java 11+  
- Apache Felix OSGi container  
- OpenMUC framework

