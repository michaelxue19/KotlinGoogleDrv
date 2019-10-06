## This project demostrated how the kotlin mulitiplaform ran on the **iOS** and **android**.


## What did it demostrate?

1) http call using ktor(1.2.4)
2) passing data in the coroutines via datachannel;
3) common kotlin calls swift code;
4) swift calls kotlin;

## What you need to do

1) Get a google service account;
2) put your private key in the 
MyMPP/MPP/SharedKotlin/src/commonMain/kotlin/com/tubi/test/interceptor/GoogleOAuth.kt
3) put your service account email in the "iss" field in the 
MyMPP/MPP/SharedKotlin/src/commonMain/kotlin/com/tubi/test/interceptor/GoogleOAuth.kt

## How to run it
1) Open 'MPP' in the android studio;
2) Open 'MPP_IOS' in the xcode;
3) edit the sharedKotlin code in the android studio;
4) build app in the android studio or xcode;
