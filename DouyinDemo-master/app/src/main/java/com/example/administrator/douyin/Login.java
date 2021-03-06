package com.example.administrator.douyin;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Looper;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.Toast;
import com.jiajie.load.LoadingDialog;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import org.jetbrains.annotations.NotNull;

import java.io.IOException;

import Controller.Constant;

import Controller.HttpUtil;
import entities.User;
import model.VideoCase;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.FormBody;
import okhttp3.RequestBody;
import okhttp3.Response;

/*
* 登录
* */
public class Login extends AppCompatActivity {
    private SharedPreferences.Editor editor;

    private EditText accountEdit;

    private EditText passwordEdit;

    private Button login;

    private final static int LOGIN_SUCCESS = 1;
    private final static int PASSWORD_WRONG = 2;
    private final static int ACCOUNT_NOT_EXIST = 3;


    private static final int OK = 200;
    private CheckBox rememberPass;
    private SharedPreferences sp;

    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);
        //getWindow().setBackgroundDrawableResource(R.drawable.login);//第二种方式设置背景图片
        sp = getSharedPreferences("info1.txt", MODE_PRIVATE);
        accountEdit = (EditText) findViewById(R.id.user);
        passwordEdit = (EditText) findViewById(R.id.pass);
        rememberPass = (CheckBox) findViewById(R.id.remember_pass);
        login = (Button) findViewById(R.id.login);
        boolean isRemember = sp.getBoolean("remember_password", false);
        if (isRemember) {
            //将账号密码都设置到文本框中
            String account = sp.getString("account", "");
            String password = sp.getString("password", "");
            accountEdit.setText(account);
            passwordEdit.setText(password);
            rememberPass.setChecked(true);
        }
        login.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                //DataCreate.initData();
                final String account = accountEdit.getText().toString();
                final String password = passwordEdit.getText().toString();

                if (TextUtils.isEmpty(account) || TextUtils.isEmpty(password)) {
                    Toast.makeText(Login.this,"账号或密码不能为空！",Toast.LENGTH_SHORT).show();
                }
                else {
                    editor = sp.edit();
                    if (rememberPass.isChecked()) {
                        editor.putBoolean("remember_password", true);
                        editor.putString("account", account);
                        editor.putString("password", password);
                    } else {
                        editor.clear();
                    }
                    editor.commit();

                    loginAsync(account, password);

                }
            }
        });

    }

    //登陆
    private void loginAsync(final String account, String password) {
        final LoadingDialog dialog = new LoadingDialog.Builder(this).loadText("加载中...").build();
        dialog.show();

        RequestBody requestBody = new FormBody.Builder()
                .add("account", account)
                .add("password", password)
                .build();
        String url = HttpUtil.rootUrl +"login";
        HttpUtil.sendPostRequest(url, requestBody, new Callback(){
            @Override
            public void onResponse(Call call, Response response) throws IOException {
                String responseData = response.body().string();
                JSONObject jsonObject = JSON.parseObject(responseData);
                runOnUiThread(() -> {
                    dialog.dismiss();
                    int responseNum = jsonObject.getInteger("result");
                    switch (responseNum) {
                        case LOGIN_SUCCESS: {
                            //Constant.currentUser = User.addUser(account);
                            Constant.currentUser=new User(jsonObject.getJSONObject("user"));
                            Intent intent = new Intent(Login.this, MainActivity.class);
                            Login.this.finish();
                            startActivity(intent);
                            //return;
                            break;
                        }
                        case PASSWORD_WRONG: {
                            Toast.makeText(Login.this, "密码错误", Toast.LENGTH_SHORT).show();
                            break;
                        }
                        case ACCOUNT_NOT_EXIST: {
                            Toast.makeText(Login.this, "账号不存在", Toast.LENGTH_SHORT).show();
                            break;
                        }
                    }
                });
            }

            @Override
            public void onFailure(Call call, IOException e) {
                Log.e("请求错误", e.getMessage());
                Looper.prepare();
                Toast.makeText(Login.this, "登录出错", Toast.LENGTH_SHORT).show();
                dialog.dismiss();
                Looper.loop();
            }
        });
        //登录的同时就获取我的视频信息
        HttpUtil.getRelevantVideo(account, new Callback() {
            @Override
            public void onResponse(@NotNull Call call, @NotNull Response response) throws IOException {
                final String responseData = response.body().string();
                JSONObject resultJSON=JSONObject.parseObject(responseData);

                JSONArray myVideoJsonArray =resultJSON.getJSONArray("uploadVideo");
                for(int i=0;i<myVideoJsonArray.size();i++){
                    JSONObject videoJSON=myVideoJsonArray.getJSONObject(i);
                    VideoCase v=new VideoCase(videoJSON);
                    Constant.currentUserVideoWorks.add(v);
                }
                JSONArray likeVideoJsonArray =resultJSON.getJSONArray("likeVideo");
                for(int i=0;i<likeVideoJsonArray.size();i++){
                    JSONObject videoJSON=likeVideoJsonArray.getJSONObject(i);
                    VideoCase v=new VideoCase(videoJSON);
                    Constant.currentUserVideoLikes.add(v);
                }
            }
            @Override
            public void onFailure(@NotNull Call call, @NotNull IOException e) {

            }
        });
    }
    // 请求注册账号
    public void OnClick_registerButton(View v)
    {
        Intent intent=new Intent();
        intent.setClass(Login.this, Register.class);
        startActivityForResult(intent, 100);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == 100) {
            if (resultCode == RESULT_OK) {
                accountEdit.setText(data.getStringExtra("account"));
                passwordEdit.setText(data.getStringExtra("password"));
            }
        }
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        String tag = intent.getStringExtra("EXIT_TAG");
        if (tag != null&& !TextUtils.isEmpty(tag)) {
            if ("SINGLETASK".equals(tag)) {//退出程序
                finish();
            }
        }
    }

}