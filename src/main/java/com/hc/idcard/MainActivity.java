package com.hc.idcard;

import android.annotation.SuppressLint;
import android.app.ProgressDialog;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.hc.pda.HcPowerCtrl;
import com.zkteco.android.IDReader.IDPhotoHelper;
import com.zkteco.android.IDReader.SoundUtil;
import com.zkteco.android.IDReader.WLTService;
import com.zkteco.android.biometric.core.device.ParameterHelper;
import com.zkteco.android.biometric.core.device.TransportType;
import com.zkteco.android.biometric.module.idcard.IDCardReader;
import com.zkteco.android.biometric.module.idcard.IDCardReaderFactory;
import com.zkteco.android.biometric.module.idcard.exception.IDCardReaderException;
import com.zkteco.android.biometric.module.idcard.meta.IDCardInfo;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;


public class MainActivity extends AppCompatActivity {

    private IDCardReader idCardReader = null;
    private ImageView imageView = null;
    private boolean bStoped = false;
    TextView tv_name, tv_ethnic, tv_year, tv_sex, tv_car_no, tv_address, tv_moth, tv_day, tv_gonganju, tv_date;
    ExecutorService mExecutorService;
    SoundUtil soundUtil;
    HcPowerCtrl ctrl;
    TextView tv_count;
    int readCount = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        initView();
        soundUtil = new SoundUtil(this);
        ctrl = new HcPowerCtrl();
        tv_count = findViewById(R.id.tv_count);
        new InitIdCardTask().execute();
    }

    private void initView() {
        tv_address = findViewById(R.id.tv_address);
        tv_car_no = findViewById(R.id.tv_car_no);
        tv_day = findViewById(R.id.tv_day);
        tv_ethnic = findViewById(R.id.tv_ethnic);
        tv_moth = findViewById(R.id.tv_moth);
        tv_sex = findViewById(R.id.tv_sex);
        tv_year = findViewById(R.id.tv_year);
        tv_name = findViewById(R.id.tv_name);
        imageView = findViewById(R.id.iv_head);
        tv_gonganju = findViewById(R.id.tv_gonganju);
        tv_date = findViewById(R.id.tv_date);
        tv_name.setText("");
        tv_year.setText("");
        tv_sex.setText("");
        tv_moth.setText("");
        tv_ethnic.setText("");
        tv_car_no.setText("");
        tv_address.setText("");
        tv_day.setText("");
        tv_date.setText("");
        tv_gonganju.setText("");

        findViewById(R.id.bt_exit).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
               exitIDCard();
                finish();
            }
        });
    }

    //HC605S 新款身份证
    private void startIDCardReader() {
        ctrl.identityPower(1);
        Map idrparams = new HashMap();
        Log.e("TAG", "startIDCardReader: ");
        idrparams.put(ParameterHelper.PARAM_SERIAL_SERIALNAME, "/dev/ttysWK2");//8.0
        idrparams.put(ParameterHelper.PARAM_SERIAL_BAUDRATE, 115200);
        idCardReader = IDCardReaderFactory.createIDCardReader(this, TransportType.SERIALPORT, idrparams);
    }


    /**
     * 退出的时候一定要下电，不然会在后台损耗大量电池电量
     */
    @Override
    protected void onDestroy() {
        super.onDestroy();
        exitIDCard();

    }

    @Override
    protected void onPause() {
        Log.e("TAG", "onPause: ");
       exitIDCard();
        super.onPause();
    }


    @Override
    protected void onResume() {
        Log.e("TAG", "onResume: ");
        ctrl.identityPower(1);
        bStoped = false;
        new InitIdCardTask().execute();
        super.onResume();
    }


    public void clearData() {
        tv_name.setText("");
        tv_year.setText("");
        tv_sex.setText("");
        tv_moth.setText("");
        tv_ethnic.setText("");
        tv_car_no.setText("");
        tv_address.setText("");
        tv_day.setText("");
        tv_date.setText("");
        tv_gonganju.setText("");
        imageView.setImageResource(0);

    }

    Handler handler = new Handler(new Handler.Callback() {
        @SuppressLint("SetTextI18n")
        @Override
        public boolean handleMessage(Message msg) {
            switch (msg.what) {
                case 0:
                    clearData();
                    Toast.makeText(getApplicationContext(), "找到身份证\n正在读取", Toast.LENGTH_LONG).show();
                    break;
                case 1:
                    clearData();
                    tv_count.setText("" + readCount++);
                    soundUtil.PlaySound(SoundUtil.SoundType.SUCCESS);
                    final IDCardInfo idCardInfo = idCardReader.getLastIDCardInfo();
                    if (idCardInfo != null) {
                        //姓名
                        String strName = idCardInfo.getName();
                        //民族
                        String strNation = idCardInfo.getNation();
                        //出生日期
                        String strBorn = idCardInfo.getBirth();
                        //住址
                        String strAddr = idCardInfo.getAddress();
                        //身份证号
                        String strID = idCardInfo.getId();
                        //有效期限
                        String strEffext = idCardInfo.getValidityTime();
                        //签发机关
                        String strIssueAt = idCardInfo.getDepart();

                        tv_name.setText(strName);
                        tv_address.setText(strAddr);
                        tv_car_no.setText(strID);
                        tv_ethnic.setText(strNation);
                        tv_sex.setText(idCardInfo.getSex());
                        String year = strBorn.substring(0, 4);
                        String month = strBorn.substring(5, 7);
                        String day = strBorn.substring(8, 10);
                        tv_moth.setText(month);
                        tv_year.setText(year);
                        tv_day.setText(day);
                        tv_gonganju.setText(strIssueAt);
                        tv_date.setText(strEffext);
                        if (idCardInfo.getPhotolength() > 0) {
                            byte[] buf = new byte[WLTService.imgLength];
                            if (1 == WLTService.wlt2Bmp(idCardInfo.getPhoto(), buf)) {
                                imageView.setImageBitmap(IDPhotoHelper.Bgr2Bitmap(buf));
                            }
                        }
                    } else {

                    }

                    break;
            }
            return false;
        }
    });

    private void readCardStart() {
        //初始化线程池 ExecutorService
        ThreadFactory threadFactory = Executors.defaultThreadFactory();
        mExecutorService = new ThreadPoolExecutor(1, 200, 1L, TimeUnit.MILLISECONDS,
                new LinkedBlockingQueue<Runnable>(1024), threadFactory, new ThreadPoolExecutor.AbortPolicy());

        mExecutorService.execute(new Runnable() {
            @Override
            public void run() {
                while (!bStoped) {
                    Log.e("TAG", "run: ");
                    try {
                        if (idCardReader.findCard(0)) {
                            handler.sendEmptyMessage(0);
                            if (idCardReader.selectCard(0)) {
                                int retType = 0;
                                retType = idCardReader.readCardEx(0, 0);
                                if (retType == 1) {//中国身份证
                                    Log.e("TAG", "中国身份证: ");
                                    handler.sendEmptyMessage(1);
                                }
                                try {
                                    //读卡时间间隔不要太短，不然调用次数过多会耗电特别快，设备发烫
                                    Thread.sleep(1000);
                                } catch (InterruptedException e) {
                                    e.printStackTrace();
                                }
                            }
                        }
                    } catch (IDCardReaderException e) {
                        Log.e("TAG", "读卡失败: " + e.getMessage());
                    }
                    try {
                        Thread.sleep(500);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            }
        });
    }

    public class InitIdCardTask extends AsyncTask<String, Integer, Boolean> {
        ProgressDialog mypDialog;

        @Override
        protected Boolean doInBackground(String... params) {
            startIDCardReader();
            try {
                idCardReader.open(0);
            } catch (IDCardReaderException e) {
                e.printStackTrace();
            }
            return true;
        }

        @Override
        protected void onPostExecute(Boolean result) {
            super.onPostExecute(result);
            mypDialog.cancel();
            readCardStart();//开始读卡
        }

        @Override
        protected void onPreExecute() {
            // TODO Auto-generated method stub
            super.onPreExecute();
            mypDialog = new ProgressDialog(MainActivity.this);
            mypDialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
            mypDialog.setMessage("init...");
            mypDialog.setCanceledOnTouchOutside(false);
            mypDialog.show();

        }
    }

    //身份证下电，关闭
    public void exitIDCard() {
        bStoped = true;
        IDCardReaderFactory.destroy(idCardReader);
        ctrl.identityPower(0);

    }
}



