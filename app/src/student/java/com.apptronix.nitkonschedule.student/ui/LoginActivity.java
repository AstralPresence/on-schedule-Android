package com.apptronix.nitkonschedule.student.ui;

import android.app.ProgressDialog;
import android.content.Intent;
import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.apptronix.nitkonschedule.R;
import com.apptronix.nitkonschedule.model.User;
import com.apptronix.nitkonschedule.student.service.AuthService;
import com.google.android.gms.auth.api.Auth;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.auth.api.signin.GoogleSignInResult;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.SignInButton;
import com.google.android.gms.common.api.GoogleApiClient;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import timber.log.Timber;

public class LoginActivity extends AppCompatActivity implements GoogleApiClient.OnConnectionFailedListener,View.OnClickListener {

    User user;
    @Subscribe(threadMode= ThreadMode.MAIN)
    public void onMessageEvent(LoginEvent event){

        Timber.i(event.getMessage());

        switch (event.getMessage()){
            case "LoginSuccessful":{

                startActivity(new Intent(this,MainActivity.class));
                finish();
                break;

            } case "LoginFailed":{

                Toast.makeText(this, R.string.server_authentication_failed, Toast.LENGTH_LONG).show();
                break;

            }case "ServerUnreachable": {

                Toast.makeText(this, R.string.server_unreachable_msg,Toast.LENGTH_LONG).show();
                break;

            }

        }
    }

    EditText _emailText;
    EditText _passwordText;
    Button loginButton;
    SignInButton signInButton;

    int RC_SIGN_IN=1;
    ProgressDialog progressDialog;
    GoogleApiClient mGoogleApiClient;

    @Override
    public void onStart() {
        super.onStart();
        EventBus.getDefault().register(this);
    }

    @Override
    public void onStop() {
        EventBus.getDefault().unregister(this);
        super.onStop();
    }

    public static final String ACTION_LOGIN = "com.apptronix.nitkonschedule.service.action.LOGIN";


    @Override
    protected void onResume() {
        super.onResume();

        user = new User(this);
        if(user.getRefreshToken()!=null){
            Timber.i("Access Token %s",user.getAccessToken());
            startActivity(new Intent(this,MainActivity.class));
            finish();
        }
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);


        _emailText = findViewById(R.id.input_email);
        _passwordText = findViewById(R.id.input_password);
        loginButton = findViewById(R.id.btn_login);
        signInButton = findViewById(R.id.sign_in_button);
        User user = new User(this);
        if(user.getRefreshToken()!=null){
            Timber.i("Access Token %s",user.getAccessToken());
            startActivity(new Intent(this,MainActivity.class));
            finish();
        }

        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestEmail()
                .requestIdToken(getString(R.string.server_client_id))
                .build();

        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .enableAutoManage(this /* FragmentActivity */, this /* OnConnectionFailedListener */)
                .addApi(Auth.GOOGLE_SIGN_IN_API, gso)
                .build();

        loginButton.setOnClickListener(this);
        signInButton.setOnClickListener(this);
    }

    public void login() {

        if (!validate()) {
            onLoginFailed();
            return;
        }



        Timber.i("Login clcikd ");
        String email = _emailText.getText().toString();
        String password = _passwordText.getText().toString();

        Intent intent = new Intent(this,AuthService.class);
        intent.putExtra("email",email);
        intent.putExtra("password",password);
        intent.setAction(AuthService.ACTION_LOGIN);
        startService(intent);

    }

    @Override
    public void onBackPressed() {
        // disable going back to the MainActivity
        moveTaskToBack(true);
    }

    public void onLoginSuccess() {
        loginButton.setEnabled(true);
        finish();
    }

    public void onLoginFailed() {
        Toast.makeText(getBaseContext(), R.string.g_login_failed, Toast.LENGTH_LONG).show();

        loginButton.setEnabled(true);
    }

    public boolean validate() {
        boolean valid = true;

        String email = _emailText.getText().toString();
        String password = _passwordText.getText().toString();

        if (email.isEmpty() || !android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            _emailText.setError(getResources().getString(R.string.invalid_email));
            valid = false;
        } else {
            _emailText.setError(null);
        }

        if (password.isEmpty() || password.length() < 4 || password.length() > 10) {
            _passwordText.setError(getResources().getString(R.string.password_error));
            valid = false;
        } else {
            _passwordText.setError(null);
        }

        return valid;
    }

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {

        Toast.makeText(this,R.string.connection_failed,Toast.LENGTH_LONG).show();

    }

    @Override
    public void onClick(View v) {

        switch (v.getId()){
            case R.id.sign_in_button:{
                signIn();
                break;
            }
            case R.id.btn_login:{
                login();
                break;
            }
        }
    }

    private void signIn() {

        Intent signInIntent = Auth.GoogleSignInApi.getSignInIntent(mGoogleApiClient);
        startActivityForResult(signInIntent, RC_SIGN_IN);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        // Result returned from launching the Intent from GoogleSignInApi.getSignInIntent(...);
        if (requestCode == RC_SIGN_IN) {
            GoogleSignInResult result = Auth.GoogleSignInApi.getSignInResultFromIntent(data);
            handleSignInResult(result);
        }
    }

    private void handleSignInResult(GoogleSignInResult result) {
        Timber.i("handleSignInResult: %d", result.getStatus().getStatusCode());
        if (result.isSuccess()) {
            // Signed in successfully, show authenticated UI.
            GoogleSignInAccount acct = result.getSignInAccount();

            String toastText="Hi " + acct.getDisplayName();
            Toast.makeText(this,toastText,Toast.LENGTH_LONG).show();

            user.makeUser(this,acct);

            Intent loginIntent = new Intent(this, AuthService.class);
            loginIntent.setAction(AuthService.ACTION_GMAIL_LOGIN);
            loginIntent.putExtra("idToken", acct.getIdToken());
            startService(loginIntent);


        } else {
            Toast.makeText(this, result.getStatus().toString(),Toast.LENGTH_LONG).show();
        }
    }

    public static class LoginEvent{

        public String message;

        public  LoginEvent(String message){
            this.message=message;
        }

        public String getMessage(){
            return message;
        }

    }
}