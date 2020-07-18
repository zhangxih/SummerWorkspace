package com.example.administrator.douyin;

import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.media.MediaPlayer;
import android.media.MediaRecorder;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.ContactsContract;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.VideoView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.FragmentTransaction;
import androidx.recyclerview.widget.OrientationHelper;
import androidx.recyclerview.widget.RecyclerView;

import com.danikula.videocache.HttpProxyCacheServer;

import org.jetbrains.annotations.NotNull;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import Controller.DataCreate;
import Controller.HttpUtil;
import butterknife.BindView;
import model.VideoCase;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.Response;


public class MainActivity extends AppCompatActivity{
    private static final String TAG = "douyin";

    private RecyclerView mRecyclerView;
    private MyAdapter mAdapter;
    MyLayoutManager2 myLayoutManager;

    private ImageButton ShootButton;
    private ImageButton SearchButton;

    private Context con = this;

    /*这里的imgs数组指定视频封面文件名，videos数组指定视频文件名，两个数组的元素按顺序对应
        图片文件存储在res/mipmap-xxhdpi中
        视频文件存储在res/raw中*/
    private int[] imgs = {R.mipmap.img_video_3, R.mipmap.img_video_2, R.mipmap.img_video_1};
    private int[] videos = {R.raw.video_3, R.raw.video_2, R.raw.video_1};
    private int index = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);


        ShootButton = (ImageButton) findViewById(R.id.shoot);
        ShootButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(MainActivity.this, PlayVideoActivity.class);
                startActivity(intent);
            }
        });
        SearchButton = (ImageButton) findViewById(R.id.tosearch);
        SearchButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(MainActivity.this, SearchActivity.class);
                startActivity(intent);
            }
        });
        TextView myInfoTextView = (TextView)findViewById(R.id.myInfo);
        myInfoTextView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(MainActivity.this, PersonInfo.class);
                startActivity(intent);
            }
        });

        //在这里获取视频和封面



        //开启子线程获取视频信息
        getVideoData();
        
        initView();
        initListener();
        initState();
    }

    private void initState() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            //透明状态栏
            getWindow().addFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);
            //透明导航栏
            getWindow().addFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_NAVIGATION);
        }
    }

    @SuppressLint("WrongConstant")
    private void initView() {
        mRecyclerView = findViewById(R.id.recycler);
        myLayoutManager = new MyLayoutManager2(this, OrientationHelper.VERTICAL, false);

        mRecyclerView.setLayoutManager(myLayoutManager);
    }

    //每次刷新视频数据重新加载适配器
    private void loadAdapter(List<VideoCase> list){
        mAdapter = new MyAdapter(this,list);
        mRecyclerView.setAdapter(mAdapter);
    }

    private void initListener() {
        myLayoutManager.setOnViewPagerListener(new OnViewPagerListener() {
            @Override
            public void onInitComplete() {

            }

            @Override
            public void onPageRelease(boolean isNext, int position) {
                Log.e(TAG, "释放位置:" + position + " 下一页:" + isNext);
                int index = 0;
                if (isNext) {
                    index = 0;
                } else {
                    index = 1;
                }
                releaseVideo(index);
            }

            @Override
            public void onPageSelected(int position, boolean bottom) {
                Log.e(TAG, "选择位置:" + position + " 下一页:" + bottom);

                playVideo(position);
            }
        });
    }

    class MyAdapter extends RecyclerView.Adapter<MyAdapter.ViewHolder> {

        private Context mContext;
        private List<VideoCase> VideoList;
        public MyAdapter(Context context,List<VideoCase> list) {
            this.mContext = context;
            this.VideoList=list;
        }

        @Override
        public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_view_pager, parent, false);
            return new ViewHolder(view);
        }

        @Override
        public void onBindViewHolder(ViewHolder holder, int position) {

            holder.videoView.setVideoPath(VideoCache.getProxy(this.mContext).getProxyUrl(VideoList.get(position).getURL()));

        }

        @Override
        public int getItemCount() {
            return VideoList.size();
        }

        public class ViewHolder extends RecyclerView.ViewHolder {
            ImageView img_thumb;
            VideoView videoView;
            ImageView img_play;
            RelativeLayout rootView;

            public ViewHolder(View itemView) {
                super(itemView);
                img_thumb = itemView.findViewById(R.id.img_thumb);
                videoView = itemView.findViewById(R.id.video_view);
                img_play = itemView.findViewById(R.id.img_play);
                rootView = itemView.findViewById(R.id.root_view);
            }
        }
    }

    private void releaseVideo(int index) {
        View itemView = mRecyclerView.getChildAt(index);
        final VideoView videoView = itemView.findViewById(R.id.video_view);
        final ImageView imgThumb = itemView.findViewById(R.id.img_thumb);
        final ImageView imgPlay = itemView.findViewById(R.id.img_play);
        videoView.stopPlayback();
        imgThumb.animate().alpha(1).start();
        imgPlay.animate().alpha(0f).start();
    }

    @TargetApi(Build.VERSION_CODES.JELLY_BEAN_MR1)
    private void playVideo(int position){
        View itemView = mRecyclerView.getChildAt(0);
        final FullWindowVideoView videoView = itemView.findViewById(R.id.video_view);
        final ImageView imgPlay = itemView.findViewById(R.id.img_play);
        final ImageView imgThumb = itemView.findViewById(R.id.img_thumb);

        final TextView comment = itemView.findViewById(R.id.comment);
        comment.setText(mAdapter.VideoList.get(position).getTitle());//在这里设置成获取视频的点赞数量
        comment.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                CommentDialog commentDialog = new CommentDialog();
                commentDialog.show(getSupportFragmentManager(), "");
            }
        });


        //final RelativeLayout rootView = itemView.findViewById(R.id.root_view);
        //final MediaPlayer mediaPlayer = new MediaPlayer();
        videoView.setOnPreparedListener(new MediaPlayer.OnPreparedListener() {
            @Override
            public void onPrepared(MediaPlayer mp) {

            }
        });

        videoView.setOnInfoListener(new MediaPlayer.OnInfoListener() {
            @Override
            public boolean onInfo(MediaPlayer mp, int what, int extra) {
                mp.setLooping(true);
                imgThumb.animate().alpha(0).setDuration(200).start();
                return false;
            }
        });
        videoView.start();

        imgPlay.setOnClickListener(new View.OnClickListener() {
            boolean isPlaying = true;

            @Override
            public void onClick(View v) {
                if (videoView.isPlaying()) {
                    imgPlay.animate().alpha(0.7f).start();
                    videoView.pause();
                    isPlaying = false;
                } else {
                    imgPlay.animate().alpha(0f).start();
                    videoView.start();
                    isPlaying = true;
                }
            }
        });
    }


/*
    @TargetApi(Build.VERSION_CODES.JELLY_BEAN_MR1)
    private void playVideo(int position) {
        View itemView = mRecyclerView.getChildAt(position);
        final FullWindowVideoView videoView = itemView.findViewById(R.id.video_view);
        final ImageView imgPlay = itemView.findViewById(R.id.img_play);
        final ImageView imgThumb = itemView.findViewById(R.id.img_thumb);
        final RelativeLayout rootView = itemView.findViewById(R.id.root_view);
        final MediaPlayer[] mediaPlayer = new MediaPlayer[1];
        videoView.setOnPreparedListener(new MediaPlayer.OnPreparedListener() {
            @Override
            public void onPrepared(MediaPlayer mp) {

            }
        });
        videoView.setOnInfoListener(new MediaPlayer.OnInfoListener() {
            @Override
            public boolean onInfo(MediaPlayer mp, int what, int extra) {
                mediaPlayer[0] = mp;
                mp.setLooping(true);
                imgThumb.animate().alpha(0).setDuration(200).start();
                return false;
            }
        });

        videoView.start();

        imgPlay.setOnClickListener(new View.OnClickListener() {
            boolean isPlaying = true;

            @Override
            public void onClick(View v) {
                if (videoView.isPlaying()) {
                    imgPlay.animate().alpha(0.7f).start();
                    videoView.pause();
                    isPlaying = false;
                } else {
                    imgPlay.animate().alpha(0f).start();
                    videoView.start();
                    isPlaying = true;
                }
            }
        });
    }
*/

    private void getVideoData(){
        final String userAccount= getSharedPreferences("info1.txt", MODE_PRIVATE).getString("account","0");
        HttpUtil.getRecommendVideo(userAccount,new Callback() {
            @Override
            public void onResponse(@NotNull Call call, @NotNull Response response) throws IOException {
                final String responseData = response.body().string();

                final List<VideoCase> VideoSource=new ArrayList<>();
                try {
                    JSONArray videoJsonArray =new JSONObject(responseData).getJSONArray("videos");//获取的视频解析数组
                    for(int i=0;i<videoJsonArray.length();i++){
                        JSONObject videoJSON=videoJsonArray.getJSONObject(i);
                        String videoTitleSource = videoJSON.getString("title");
                        String videoDescriptionSource = videoJSON.getString("info");
                        String videoURLSource = videoJSON.getString("url");
                        VideoCase v=new VideoCase(videoTitleSource,videoDescriptionSource,videoURLSource);
                        VideoSource.add(v);
                    }

                } catch (JSONException e) {
                    e.printStackTrace();
                }
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        final List<VideoCase> vs=VideoSource;
                        loadAdapter(vs);
                    }
                });
            }
            @Override
            public void onFailure(@NotNull Call call, @NotNull IOException e) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        Toast.makeText(MainActivity.this,"连接失败,播放本地视频！！！",Toast.LENGTH_LONG).show();
                        //暂时不写
                    }
                });
            }
        });
    }


}