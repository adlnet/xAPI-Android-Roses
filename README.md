xAPI-Android-Roses
==================

This app follows the ADL SCORM Profile for xAPI enabled learning content that allows organizations to incrementally transition from a centralized SCORM LMS to diverse and flexible systems without the loss of interoperability. As a user progresses through the different modules, statements are recorded that contain the same data that SCORM modules also collect. For more reading on the SCORM Profile please view it [here](https://github.com/adlnet/xAPI-SCORM-Profile/blob/master/xapi-scorm-profile.md#20-when-to-use-this-profile).

#Installation
You can install the application on Android directly from the Google Play store [here](https://play.google.com/store/apps/details?id=org.adl.roses&hl=en)

#Configuration
When you first open the app it will ask you to provide a name and email; this will create an actor for the statements. Once you are signed in, the app will look for any previous attempts via xAPI's activity state API. If found, it will ask you if you'd like to continue from the point you last suspended the module.

#Use
Each module has three slides that the user can maneuver through using the Prev/Next buttons. If you'd like to suspend the module just push the Suspend button.

#Contributing to the Project
To begin developing and running the app, create a folder named `libs` and place it in the `src/` folder. After that download the latest jXAPI [release](https://search.maven.org/#search%7Cga%7C1%7Cjxapi) and place the jar-with-dependencies jar in the `libs/` folder. Build the project and you're ready to go.

## License
   Copyright &copy;2016 Advanced Distributed Learning

   Licensed under the Apache License, Version 2.0 (the "License");
   you may not use this file except in compliance with the License.
   You may obtain a copy of the License at

       http://www.apache.org/licenses/LICENSE-2.0

   Unless required by applicable law or agreed to in writing, software
   distributed under the License is distributed on an "AS IS" BASIS,
   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
   See the License for the specific language governing permissions and
   limitations under the License.
