package com.eliconcepts.mymissal;

import android.content.Intent;
import android.os.CountDownTimer;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.AppCompatEditText;
import android.util.Log;
import android.view.View;
import android.view.animation.AnimationUtils;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.goodiebag.pinview.Pinview;
import com.lamudi.phonefield.PhoneEditText;
import com.lamudi.phonefield.PhoneInputLayout;
import com.rilixtech.CountryCodePicker;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.FirebaseException;
import com.google.firebase.FirebaseTooManyRequestsException;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.PhoneAuthCredential;
import com.google.firebase.auth.PhoneAuthProvider;
import com.tuyenmonkey.mkloader.model.Line;

import java.util.concurrent.TimeUnit;

public class PhoneActivity extends AppCompatActivity {

    private static final String TAG = "PhoneAuthActivity";

    private static final String KEY_VERIFY_IN_PROGRESS = "key_verify_in_progress";

    private static final int STATE_INITIALIZED = 1;
    private static final int STATE_CODE_SENT = 2;
    private static final int STATE_VERIFY_FAILED = 3;
    private static final int STATE_VERIFY_SUCCESS = 4;
    private static final int STATE_SIGNIN_FAILED = 5;
    private static final int STATE_SIGNIN_SUCCESS = 6;
    private boolean mVerificationInProgress = false;
    private String mVerificationId;
    private PhoneAuthProvider.ForceResendingToken mResendToken;
    private PhoneAuthProvider.OnVerificationStateChangedCallbacks mCallbacks;

    // [START declare_auth]
    private FirebaseAuth mAuth;
    // [END declare_auth]



    private CountryCodePicker ccp;

    private LinearLayout loadingProgress;
    private Button loginButton;
    private AppCompatEditText phoneNumber;
    private LinearLayout verifyLayout;
    private LinearLayout inputCodeLayout;
    private TextView timer;
    private Button resendCode;
    private Pinview smsCode;
    private String phone;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_phone);
        getWindow().setBackgroundDrawableResource(R.drawable.gradiennt1);

        //define views here
        inputCodeLayout = (LinearLayout) findViewById(R.id.inputCodeLayout);
        loadingProgress = (LinearLayout)findViewById(R.id.loadingProgress);
        loadingProgress.setVisibility(View.INVISIBLE);
        verifyLayout = (LinearLayout)findViewById(R.id.verifyLayout);
        ccp = (CountryCodePicker) findViewById(R.id.ccp);
        loginButton = (Button)findViewById(R.id.loginButton);
        phoneNumber = (AppCompatEditText)findViewById(R.id.phone_number);
        timer = (TextView) findViewById(R.id.timer);
        resendCode = (Button) findViewById(R.id.resend_code);
        smsCode = (Pinview) findViewById(R.id.sms_code);


        showView(verifyLayout); //show the main layout
        hideView(inputCodeLayout); //hide the otp layout
        hideView(loadingProgress); //hide the progress loading layout


        //set onclick listener for login button
        loginButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                //this method is triggered when the login button is clicked
             attemptLogin();

            }

        });
        resendCode.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                // this method is triggered when the resend code button is pressed
                retryVerify();
            }
        });
        // [START initialize_auth]
        mAuth = FirebaseAuth.getInstance();
// [END initialize_auth]
        mCallbacks = new PhoneAuthProvider.OnVerificationStateChangedCallbacks() {

            @Override
            public void onVerificationCompleted(PhoneAuthCredential credential) {
                // This callback will be invoked in two situations:
                // 1 - Instant verification. In some cases the phone number can be instantly
                //     verified without needing to send or enter a verification code.
                // 2 - Auto-retrieval. On some devices Google Play services can automatically
                //     detect the incoming verification SMS and perform verificaiton without
                //     user action.
                Log.d(TAG, "onVerificationCompleted:" + credential);
                //sign in user to new Activity here
                signInWithPhoneAuthCredential(credential);
            }

            @Override
            public void onVerificationFailed(FirebaseException e) {
                // This callback is invoked in an invalid request for verification is made,
                // for instance if the the phone number format is not valid.
                Log.w(TAG, "onVerificationFailed", e);

                if (e instanceof FirebaseAuthInvalidCredentialsException) {
                    // Invalid request
                    // ...
                } else if (e instanceof FirebaseTooManyRequestsException) {
                    // The SMS quota for the project has been exceeded
                    // ...
                }

                // Show a message and update the UI
                // ...
            }

            @Override
            public void onCodeSent(String verificationId,
                                   PhoneAuthProvider.ForceResendingToken token) {
                // The SMS verification code has been sent to the provided phone number, we
                // now need to ask the user to enter the code and then construct a credential
                // by combining the code with a verification ID.
                Log.d(TAG, "onCodeSent:" + verificationId);

                // Save verification ID and resending token so we can use them later
                mVerificationId = verificationId;
                mResendToken = token;


                // ...
            }
        };
        smsCode.setPinViewEventListener(new Pinview.PinViewEventListener() {
            @Override
            public void onDataEntered(Pinview pinview, boolean b) {

                //trigger this when the OTP code has finished typing
                final String verifyCode = smsCode.getValue();
                verifyPhoneNumberWithCode(mVerificationId,verifyCode);
            }
        });

      //onCreate ends here
    }

    private void retryVerify() {
        resendVerificationCode(phone,mResendToken);
    }


    private void verifyPhoneNumberWithCode(String verificationId, String code) {
        hideView(verifyLayout); //hide the main layout
        hideView(inputCodeLayout); //hide the otp layout
        showView(loadingProgress); //show the progress loading layout


        // [START verify_with_code]
        PhoneAuthCredential credential = PhoneAuthProvider.getCredential(verificationId, code);
        // [END verify_with_code]
        signInWithPhoneAuthCredential(credential);
    }

    private void resendVerificationCode(String phoneNumber,
                                        PhoneAuthProvider.ForceResendingToken token) {
        PhoneAuthProvider.getInstance().verifyPhoneNumber(
                phoneNumber,        // Phone number to verify
                60,                 // Timeout duration
                TimeUnit.SECONDS,   // Unit of timeout
                this,               // Activity (for callback binding)
                mCallbacks,         // OnVerificationStateChangedCallbacks
                token);             // ForceResendingToken from callbacks
    }


    private void attemptLogin() {

        //reset any erros
        phoneNumber.setError(null);

        //get values from phone edit text and pass to countryPicker
        ccp.registerPhoneNumberTextView(phoneNumber);
        phone = ccp.getFullNumber();

        boolean cancel= false;
        View focusView = null;

        //check if phone number is valid: I would just check the length
                if(!isPhoneValid(phone)){

                    focusView=phoneNumber;
                    cancel=true;
                }

            if (cancel){
                //there was an error in the length of phone
                focusView.requestFocus();
            }else{

                //show loading screen
                hideView(verifyLayout);
                showView(inputCodeLayout);
                hideView(loadingProgress);

                //go ahead and verify number
                startPhoneNumberVerification(phone);
                //time to show retry button
                new CountDownTimer(45000, 1000) {
                    @Override
                    public void onTick(long l) {
                        timer.setText("0:" + l / 1000 + " s");
                        resendCode.setVisibility(View.INVISIBLE);
                    }

                    @Override
                    public void onFinish() {
                        timer.setText(0 + " s");
                        resendCode.startAnimation(AnimationUtils.loadAnimation(PhoneActivity.this, R.anim.slide_from_right));
                        resendCode.setVisibility(View.VISIBLE);
                    }
                }.start();
                //timer ends here
            }


    }

    private boolean isPhoneValid(String phone) {
        return phone.length() > 8;
    }


    private void startPhoneNumberVerification(String phoneNumber) {
        // [START start_phone_auth]
        PhoneAuthProvider.getInstance().verifyPhoneNumber(
                phoneNumber,        // Phone number to verify
                60,                 // Timeout duration
                TimeUnit.SECONDS,   // Unit of timeout
                this,               // Activity (for callback binding)
                mCallbacks);        // OnVerificationStateChangedCallbacks
        // [END start_phone_auth]

        mVerificationInProgress = true;
    }
    private void signInWithPhoneAuthCredential(PhoneAuthCredential credential) {

        mAuth.signInWithCredential(credential)
                .addOnCompleteListener(this, new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task) {
                        if (task.isSuccessful()) {
                            // Sign in success, update UI with the signed-in user's information
                            Log.d(TAG, "signInWithCredential:success");
            //user phone number has been verified, what next?
                            FirebaseUser user = task.getResult().getUser();
                            Intent i = new Intent(PhoneActivity.this,MainActivity.class);
                            startActivity(i);
                                                        //its best you store the userID or details in shared preferences and store something in a shared pref to show the user has already logged in. then continue from there. you dont want users to be verifying their number all the time.
                            //go to next activity or do whatever you like

                            // ...
                        } else {
                            // Sign in failed, display a message and update the UI
                            Log.w(TAG, "signInWithCredential:failure", task.getException());

                            if (task.getException() instanceof FirebaseAuthInvalidCredentialsException) {
                                // The verification code entered was invalid
                                Toast.makeText(PhoneActivity.this,"Invalid Verification Code",Toast.LENGTH_LONG).show();
                            }
                        }
                    }
                });
    }
    private void showView (View... views){
        for(View v: views){
            v.setVisibility(View.VISIBLE);

        }

    }
    private void hideView (View... views){
        for(View v: views){
            v.setVisibility(View.INVISIBLE);

        }

    }


}
