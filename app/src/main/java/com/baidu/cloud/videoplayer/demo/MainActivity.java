package com.baidu.cloud.videoplayer.demo;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;

import com.baidu.cloud.videoplayer.demo.info.VideoInfo;
import com.baidu.cloud.media.download.VideoDownloadManager;

import android.annotation.TargetApi;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.BaseAdapter;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

public class MainActivity extends Activity {
    private static final String TAG = "MainActivity";
    private ListView listView;
    private ArrayList<VideoInfo> listData = new ArrayList<VideoInfo>();

    public static final String SAMPLE_USER_NAME = "sampleUser";
    VideoDownloadManager downloadManagerInstance;

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // 设置状态栏颜色
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            Window window = this.getWindow();
            window.clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);
            window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
            // finally change the color
            window.setStatusBarColor(0xfff7f7f7);
        }
        setContentView(R.layout.activity_main);

        downloadManagerInstance = VideoDownloadManager.getInstance(MainActivity.this, MainActivity.SAMPLE_USER_NAME);

        listView = (ListView) this.findViewById(R.id.lv_video_list);
        listView.setAdapter(adapter);
    }

    @Override
    protected void onResume() {
        Log.d(TAG, "onResume");
        refreshData();
        super.onResume();

    }

    public void refreshData() {
        listData = getMainSampleData(this);
        adapter.notifyDataSetChanged();
    }

    /**
     * 注：以下adapter仅为简单实现，实际项目中需要进行优化
     */
    private BaseAdapter adapter = new BaseAdapter() {

        private LayoutInflater mInflater;

        @Override
        public int getCount() {
            return listData.size();
        }

        @Override
        public Object getItem(int position) {
            return null;
        }

        @Override
        public long getItemId(int position) {
            return 0;
        }

        @Override
        public View getView(final int position, View convertView, ViewGroup parent) {
            if (mInflater == null) {
                mInflater = LayoutInflater.from(MainActivity.this);
            }
            if (convertView == null) {
                convertView = mInflater.inflate(R.layout.item_of_list_video, null);
            }
            final VideoInfo info = listData.get(position);
            TextView tvTitle = (TextView) convertView.findViewById(R.id.tv_item_title);
            TextView tvDesc = (TextView) convertView.findViewById(R.id.tv_item_desc);
            ImageView ivIcon = (ImageView) convertView.findViewById(R.id.iv_item_icon);
            ImageButton ibtnDelete = (ImageButton) convertView.findViewById(R.id.ibtn_item_delete);
            ImageButton ibtnDownload = (ImageButton) convertView.findViewById(R.id.ibtn_item_download);

            if (info.getImageUrl() != null && !info.getImageUrl().equals("")) {
                // fetch image from assets
                // if your image from url, you need to fetch image async
                InputStream ims;
                try {
                    ims = getAssets().open(info.getImageUrl());
                    Drawable iconDrawable = Drawable.createFromStream(ims, null);
                    ivIcon.setImageDrawable(iconDrawable);
                } catch (IOException e) {
                    e.printStackTrace();
                }
                
            } else {
                ivIcon.setImageResource(R.drawable.item_default_icon);
            }
            tvTitle.setText(info.getTitle());
            tvDesc.setText(info.getUrl());

            convertView.setOnClickListener(new OnClickListener() {

                @Override
                public void onClick(View v) {
                    Intent intent;
                    intent = new Intent(MainActivity.this, SimplePlayActivity.class);
                    intent.putExtra("videoInfo", info);
                    startActivity(intent);
                }

            });

            return convertView;
        }

    };

    /**
     * 初次进入应用，SP无数据时，准备样例数据
     *
     * @return
     */
    public ArrayList<VideoInfo> getMainSampleData(Context context) {
        ArrayList<VideoInfo> sampleList = new ArrayList<VideoInfo>();

        String title1 = "百度云宣传视频";
        String url1 = "https://vd2.bdstatic.com/mda-ja0x1ixwjw4tfn14/hd/mda-ja0x1ixwjw4tfn14.mp4";
        VideoInfo info1 = new VideoInfo(title1, url1);
        info1.setCanDelete(false);
        info1.setImageUrl("baidu_cloud_bigger.jpg");
        sampleList.add(info1);

//        String title2 = "LSS3.0使用说明";
//        String url2 = "http://gkkskijidms30qudc3v.exp.bcevod.com/mda-gkks7fejzyj89qkf/mda-gkks7fejzyj89qkf.m3u8";
//        VideoInfo info2 = new VideoInfo(title2, url2);
//        info2.setCanDelete(false);
//        info2.setImageUrl("baidu_cloud_lss3_release.jpg");
//        sampleList.add(info2);

        String title3 = "直播链接(HLS/RTMP/HTTP-FLV均可播放)";
        String url3 = "http://pull3.gz.bigenemy.cn/live/vivoteststream.flv";
        VideoInfo info3 = new VideoInfo(title3, url3);
        info3.setCanDelete(false);
        sampleList.add(info3);

        String title4 = "直播链接是您推流对应的播放链接";
        String url4 = "";
        VideoInfo info4 = new VideoInfo(title4, url4);
        info4.setCanDelete(false);
        sampleList.add(info4);

        return sampleList;
    }
}