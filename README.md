# MADAssistant Client SDK
Mobile Android Debugging Assistant.
This is the SDK for integration in client applications.

## Table of Contents
1. [What is MADAssistant](#what-is-madassistant)
2. [Overview](#overview)
3. [Repository App](#repository-app)
5. [Security](#security)
6. [Usage](#usage)
7. [Sample App](#sample-app)

## What is MADAssistant?
- A logging library that transmits the logs to a companion app on the same device.
- Logs are:
  - Grouped by session
  - Timestamped
  - Easily identifiable by type and subtype
  - Exportable by email or other share methods
- **Can be integrated into release builds with complete information security, owing to the auth-token generator.**

## Overview
- **Comprehensive logging solution**  
  Allows developers to log network calls, analytics sdk calls, exceptions, crashes and even generic log messages.  
  These logs are timestamped and displayed in a chronological manner, enabling easy audit.  

- **Remote issue debugging**  
  Debugging issues on remote devices is currently a time-consuming affair.  

  It generally involves the following steps by a developer:
  - Halting work on their current project.
  - Reverting their code to a state in which the issue was encountered.
  - Reproducing the issue 
  - Identifying the issue and resolving it  
  
  These steps are fraught with problems like:
  - Identification of the correct developer to handle an issue based on skillset and knowledge.
  - Downtime for the developer due to work halt and code revert.
  - Inability to revert the code to the required state due to tooling constraints.
  - Inability to reproduce the issue due to device/environment specific issues.

  The library helps reduce the need for the first three steps by enabling the developer to get appropriate logs from the user, which provides a way to backtrack the cause.  
  This significantly reduces the effort and time required by the developer to debug an issue.
- **Completely on-device solution**  
  The logs are stored on the user's device and not automatically mailed elsewhere.  
  This ensures information security and negates the need for any storage service fees.
- **Companion storage and viewer application**  
  The logs generated are stored on a companion app, ensuring that the logs are not bloating up the client app.  
  Logs are stored indefinitely, enabling easy audit at a later date.  
  The companion app also provides a clean and structured way to view the logs, with a session-based and timestamped display to simplify debugging.  
- **Analytics Audit**  
  By providing an easy method to view analytics calls, the system aids auditing of tracked events.
- **Log Sharing**  
  Logs generated from the sdk can be shared via any share method that accepts a file.  
  Entire sessions may also be shared to help pinpoint issues with the least amount of friction.
- **Usable in release builds**  
  By utilising a password-based, device specific authentication system, logs can be limited to specific devices.  
  Logs can further be controlled by using a fine-grained permission system that pinpoints what logs are filtered.  
  Logs can also be encrypted, disabling viewing on the remote device. These logs can be shared with the developer to debug an issue.
  

## Repository App
- This serves as repository and viewer for all the logs generated by the library.
- Provides a session-wise display of the logs
- All logs are stored only on the device and not sent anywhere
- Provides the option to mail a single log or an entire session via share  
[Download from Play Store](https://play.google.com/store/apps/details?id=com.evdayapps.madassistant.repository)
### Screenshots
#### Home Screen 
<img width="19%" alt="portfolio_view" src="doc/client_list.png"> <img width="19%" alt="portfolio_view" src="doc/device_info.png"> <img width="19%" alt="portfolio_view" src="doc/dev_portal.png">  

#### Client Details / Session List
<img width="19%" alt="portfolio_view" src="doc/session_list.png"> <img width="19%" alt="portfolio_view" src="doc/changelog_list.png">  

#### Session Details / Log List
<img width="19%" alt="portfolio_view" src="doc/log_list_unfiltered_1.png"> <img width="19%" alt="portfolio_view" src="doc/loglist_unfiltered_2.png"> <img width="19%" alt="portfolio_view" src="doc/session_details_filtered.png">   

#### Log Viewer
<img width="19%" alt="portfolio_view" src="doc/network_details.png"> <img width="19%" alt="portfolio_view" src="doc/stacktrace_0.png"> <img width="19%" alt="portfolio_view" src="doc/stacktrace_1.png"> <img width="19%" alt="portfolio_view" src="doc/stacktrace_2.png"> <img width="19%" alt="portfolio_view" src="doc/analytics_details.png"> 


## Security
An auth-token based system that controls what logs the user may view based on:
- The user's installation identifier (a uuid generated by the MADAssistant app once per installation)
- An optional start and end time
- The logs accessible to the user (with fine-grained regex-based filters).  
  This information is encrypted using a passphrase which needs to provided to this library on runtime, to decrypt. 
- All filtering is performed within the client library and then sent to the MADAssistant app
- **Logs may also be encrypted, preventing the user from viewing the logs, but able to still share them with the developer. This enables usage of the library in release builds, without the risk of leaking information**

<img width="19%" alt="authtoken-generator-1" src="doc/Screenshot_20220408-213416.png"> <img width="19%" alt="portfolio_view" src="doc/auth_gen_2.png">

NOTE: These logs are not uploaded to any server and will remain only on the user's device.


## Usage
- Add the jitpack repository to project build.gradle
  ```
  allprojects {
	repositories {
		...
		maven { url 'https://jitpack.io' }
	}
  }
  ```
- Add the library as a dependency
  ```
  implementation 'com.github.Evdayapps:madassistant-clientsdk:<latest-version>'
  ```
- Create an instance of MADAssistantClient
  ```
  val logUtils = object : Logger {
            override fun i(tag: String, message: String) {
                Log.i(tag, message)
            }

            override fun v(tag: String, message: String) {
                Log.v(tag, message)
            }

            override fun d(tag: String, message: String) {
                Log.d(tag, message)
            }

            override fun e(throwable: Throwable) {
                throwable.printStackTrace()
            }
        }

        madAssistantClient = MADAssistantClientImpl(
            applicationContext = applicationContext,
            passphrase = "<enter your passphrase here>",
            logger = logUtils,
            callback = object : MADAssistantClient.Callback {
                override fun onSessionStarted(sessionId: Long) {
                    Log.i("MADAssistant","Session Started")
                }

                override fun onSessionEnded(sessionId: Long) {}

                override fun onConnected() {
                    Log.i("MADAssistant","Connected")
                }

                override fun onDisconnected(code: Int, message: String) {}

            }
        )

        // Bind the client to the remote service
        madAssistantClient.connect()

        // Start a session
        madAssistantClient.startSession()

        // Test log
        madAssistantClient.logGenericLog(Log.INFO,"Test","Just a test")
  ```
- Log events using the appropriate method
  ```
  // For Exceptions
  fun logException(throwable: Throwable, message: String?, data: Map<String, Any?>?)
  
  // For Analytics
  fun logAnalyticsEvent(destination: String, eventName: String, data: Map<String, Any?>)
  
  // For logs that match android.util.Log
  fun logGenericLog(type: Int, tag: String, message: String, data: Map<String, Any?>?)

  // For network logs. 
  fun logNetworkCall(data: NetworkCallLogModel)
  Or use MADAssistantOkHttp3Interceptor as an interceptor in OkHttp3 setups
  ```
- Logging Crashes
  ```
  client.logCrashes()
  ```
  Alternately, crashes can be logged manually too using:
  ```
  client.logCrashReport(throwable: Throwable)
  ```
 
## Sample App
The code for a test client is available within the `testapp` folder  
Can also be downloaded from [Play Store](https://play.google.com/store/apps/details?id=com.evdayapps.madassistant.testapp)


## License
```
    Copyright (C) EvdayApps.
    Copyright (C) Shannon Rodricks.

    Licensed under the Apache License, Version 2.0 (the "License");
    you may not use this file except in compliance with the License.
    You may obtain a copy of the License at

       http://www.apache.org/licenses/LICENSE-2.0

    Unless required by applicable law or agreed to in writing, software
    distributed under the License is distributed on an "AS IS" BASIS,
    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
    See the License for the specific language governing permissions and
    limitations under the License.
```







