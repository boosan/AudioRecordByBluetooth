package com.bosan.audiorecordbybluetooth;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothProfile;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.media.MediaRecorder;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.os.ParcelUuid;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Method;
import java.util.Set;

public class MainActivity extends AppCompatActivity {
    private static final String TAG = "MainActivity";
    private AudioManager audioManager;
    private MediaRecorder recorder;
    private TextView textFilePath, textStatus,textInfo;

    Button btnRecord;

    private String mPath;

    BluetoothAdapter mBluetoothAdapter = null;

    private Handler mHandler = new Handler(){
        @Override
        public void handleMessage(@NonNull Message msg) {

        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        audioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);

        textFilePath = (TextView) findViewById(R.id.text_file_path);
        textStatus = (TextView) findViewById(R.id.text_status);
        textInfo = (TextView) findViewById(R.id.text_info);

        btnRecord = findViewById(R.id.btn_record);

        btnRecord.setOnClickListener(clickListener);
        findViewById(R.id.btn_stop).setOnClickListener(clickListener);
        findViewById(R.id.btn_play).setOnClickListener(clickListener);
        findViewById(R.id.btn_stop_play).setOnClickListener(clickListener);

        checkPermission();

        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        if (mBluetoothAdapter == null) {
            // 说明此设备不支持蓝牙操作
        }
        scan();

    }

    private boolean mIsScan = true;
    ScanRunnable scanRunnable = new ScanRunnable();
    @Override
    protected void onResume() {
        super.onResume();
        mIsScan = true;
        mHandler.postDelayed(scanRunnable,5 * 1000);
    }

    @Override
    protected void onPause() {
        mIsScan = false;
        mHandler.removeCallbacks(scanRunnable);
        super.onPause();
    }

    class ScanRunnable implements Runnable{

        @Override
        public void run() {
            scan();
            if(mIsScan) {
                mHandler.postDelayed(scanRunnable, 5 * 1000);
            }
        }
    }


    private void scan() {
        if(!mIsScan){
            Log.i(TAG,"停止刷新信息！");
            return;
        }
        Log.i(TAG,"刷新信息！");

        boolean isBluetooth = false;
        if (mBluetoothAdapter != null & mBluetoothAdapter.isEnabled()) {
//            int a2dp = mBluetoothAdapter.getProfileConnectionState(BluetoothProfile.A2DP); // 可操控蓝牙设备，如带播放暂停功能的蓝牙耳机
//            int headset = mBluetoothAdapter.getProfileConnectionState(BluetoothProfile.HEADSET); // 蓝牙头戴式耳机，支持语音输入输出
//            int health = mBluetoothAdapter.getProfileConnectionState(BluetoothProfile.HEALTH); // 蓝牙穿戴式设备
//            int GATT = mBluetoothAdapter.getProfileConnectionState(BluetoothProfile.GATT);
//            Log.e("lqq", "a2dp=" + a2dp + ",headset=" + headset + ",health=" + health);
            // 查看是否蓝牙是否连接到三种设备的一种，以此来判断是否处于连接状态还是打开并没有连接的状态
            Set<BluetoothDevice> pairedDevices = mBluetoothAdapter.getBondedDevices();
            if (pairedDevices.size() > 0) {
                for (BluetoothDevice device : pairedDevices) {
                    device.getC
                    // 把名字和地址取出来添加到适配器中
                    //得到BluetoothDevice的Class对象
                    Class<BluetoothDevice> bluetoothDeviceClass = BluetoothDevice.class;
                    try {//得到连接状态的方法
                        if(device != null) {
                            if (device.getName() != null) {
                                //AB030110152233
                                Method method = bluetoothDeviceClass.getDeclaredMethod("isConnected", (Class[]) null);
                                //打开权限
                                method.setAccessible(true);
                                Boolean invoke = (Boolean) method.invoke(device, (Boolean[]) null);
                                if(device.getName().startsWith("AB") && invoke) {
                                    Log.i(TAG, device.getName() + "-" + device.getAddress() + "-" + device.getBondState() + "-" + device.getType() + "-isConnected-" + invoke);
//                                    String connectStr = invoke == true ? "是" : "否";
                                    textInfo.setText("蓝牙名称："+device.getName()
                                            +"\nMAC地址："+device.getAddress()
//                                            +"\n是否连接："+connectStr
                                    );
                                    isBluetooth = true;
                                }
                            }
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }
        } else {// 没有开始蓝牙
//            Toast.makeText(this, "蓝牙未打开！", Toast.LENGTH_LONG).show();
        }
        if(isBluetooth){
            btnRecord.setText("录音（蓝牙耳机）");
        }else {
            btnRecord.setText("录音（安卓设备）");
            textInfo.setText("");
        }

    }


    @Override
    protected void onDestroy() {
        super.onDestroy();
        stopRecord();
        stopPlay();
    }


    private void startRecord() {
        File path = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS) + "/AudioRecordTest");
        if (!path.exists())
            path.mkdirs();

//        stopRecord();

        try {

            File myfile = getApplicationContext().getExternalCacheDir();
            Log.i(TAG, "exists: " + myfile.exists());
            Log.i(TAG, "startRecord: " + myfile.getAbsolutePath());
            File file = File.createTempFile("audio_", ".m4a", myfile);

            audioManager.startBluetoothSco();
            recorder = new MediaRecorder();
            recorder.setAudioSource(MediaRecorder.AudioSource.DEFAULT);
            recorder.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4);
            recorder.setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB);
            recorder.setOutputFile(file.toString());
            recorder.prepare();
            recorder.start();

            textFilePath.setText("文件存储路径" + file.getAbsolutePath());
            mPath = file.getAbsolutePath();
            textStatus.setText("录制中...");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    private void stopRecord() {
        try {
            if(audioManager != null) {
                audioManager.stopBluetoothSco();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        try {
            if(recorder != null) {
                recorder.stop();
                recorder.release();
                recorder = null;
                textStatus.setText("停止录制");
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private View.OnClickListener clickListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            if (v.getId() == R.id.btn_record)
                startRecord();
            else if (v.getId() == R.id.btn_stop)
                stopRecord();
            else if (v.getId() == R.id.btn_play)
                play();
            else if (v.getId() == R.id.btn_stop_play)
                stopPlay();
        }
    };

    MediaPlayer mediaPlayer = null;

    private void stopPlay() {
        if(mediaPlayer != null){
            try {
                mediaPlayer.stop();
                mediaPlayer.release();
                mediaPlayer = null;
            }catch (Exception e){
                e.printStackTrace();
            }
        }
    }

    private void play() {
        if (mPath != null) {
            File file = new File(mPath);
            if (file.exists()) {
                mediaPlayer = new MediaPlayer();
                mediaPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);
                Uri myUri = Uri.fromFile(file);
                try {
                    mediaPlayer.setDataSource(getApplicationContext(), myUri);
                    mediaPlayer.prepare();
                    mediaPlayer.start();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }

    }


    public void checkPermission() {
        int targetSdkVersion = 0;
        String[] PermissionString = {
                Manifest.permission.RECORD_AUDIO,
                Manifest.permission.WRITE_EXTERNAL_STORAGE,
                Manifest.permission.READ_EXTERNAL_STORAGE,
                Manifest.permission.MODIFY_AUDIO_SETTINGS,
                Manifest.permission.BLUETOOTH,
                Manifest.permission.BLUETOOTH_ADMIN,
                Manifest.permission.BROADCAST_STICKY
        };
        try {
            final PackageInfo info = this.getPackageManager().getPackageInfo(this.getPackageName(), 0);
            targetSdkVersion = info.applicationInfo.targetSdkVersion;//获取应用的Target版本
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
//            Log.e("err", "检查权限_err0");
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            //Build.VERSION.SDK_INT是获取当前手机版本 Build.VERSION_CODES.M为6.0系统
            //如果系统>=6.0
            if (targetSdkVersion >= Build.VERSION_CODES.M) {
                //第 1 步: 检查是否有相应的权限
                boolean isAllGranted = checkPermissionAllGranted(PermissionString);
                if (isAllGranted) {
                    //Log.e("err","所有权限已经授权！");
                    return;
                }
                // 一次请求多个权限, 如果其他有权限是已经授予的将会自动忽略掉
                ActivityCompat.requestPermissions(this,
                        PermissionString, 1);
            }
        }
    }


    /**
     * 检查是否拥有指定的所有权限
     */
    private boolean checkPermissionAllGranted(String[] permissions) {
        for (String permission : permissions) {
            if (ContextCompat.checkSelfPermission(this, permission) != PackageManager.PERMISSION_GRANTED) {
                // 只要有一个权限没有被授予, 则直接返回 false
                //Log.e("err","权限"+permission+"没有授权");
                return false;
            }
        }
        return true;
    }

    //申请权限结果返回处理
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == 1) {
            boolean isAllGranted = true;
            // 判断是否所有的权限都已经授予了
            for (int grant : grantResults) {
                if (grant != PackageManager.PERMISSION_GRANTED) {
                    isAllGranted = false;
                    break;
                }
            }
            if (isAllGranted) {
                // 所有的权限都授予了
                Log.e("err", "权限都授权了");
            } else {
                // 弹出对话框告诉用户需要权限的原因, 并引导用户去应用权限管理中手动打开权限按钮
                //容易判断错
                //MyDialog("提示", "某些权限未开启,请手动开启", 1) ;
            }
        }
    }
}