package com.finogeeks.finochatapp.modules.home;

import android.annotation.SuppressLint;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.RelativeLayout;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AlertDialog;
import androidx.core.view.GravityCompat;
import androidx.fragment.app.Fragment;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import androidx.viewpager.widget.ViewPager;

import com.finogeeks.finochat.modules.base.BaseActivity;
import com.finogeeks.finochat.components.content.ScreenShotListenManager;
import com.finogeeks.finochat.repository.ThemeKit;
import com.finogeeks.finochat.sdk.FinoCallBack;
import com.finogeeks.finochat.sdk.FinoChatClient;
import com.finogeeks.finochat.sdk.FinoChatOption;
import com.finogeeks.finochat.sdk.INetworkManager;
import com.finogeeks.finochat.sdk.SimpleCallBack;
import com.finogeeks.finochat.services.IBadgeManager;
import com.finogeeks.finochat.services.ServiceFactory;
import com.finogeeks.finochat.utils.FloatWindowPermissionHelper;
import com.finogeeks.finochat.utils.Log;
import com.finogeeks.finochat.widget.FeedBackFloatingView;
import com.finogeeks.finochatapp.BuildConfig;
import com.finogeeks.finochatapp.R;
import com.finogeeks.finochatapp.modules.update.model.Data;
import com.finogeeks.finochatapp.modules.update.model.VersionResp;
import com.finogeeks.finochatapp.utils.ForegroundCallbacks;
import com.finogeeks.finochatapp.utils.MemoryLeakUtil;
import com.finogeeks.finochatapp.views.TabContainerView;
import com.finogeeks.utility.utils.ResourceKt;
import com.finogeeks.utility.views.FinDrawerLayout;
import com.google.gson.Gson;
import com.tencent.bugly.crashreport.CrashReport;

import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.text.StrBuilder;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

import io.reactivex.Observable;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.Disposable;
import io.reactivex.schedulers.Schedulers;
import kotlin.text.StringsKt;
import me.leolin.shortcutbadger.ShortcutBadger;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.ResponseBody;

import static androidx.drawerlayout.widget.DrawerLayout.LOCK_MODE_LOCKED_CLOSED;
import static androidx.drawerlayout.widget.DrawerLayout.LOCK_MODE_UNLOCKED;

@SuppressLint("CheckResult")
public class HomeActivity extends BaseActivity implements
        TabContainerView.EventListener,
        INetworkManager.NetworkEventListener,
        ForegroundCallbacks.Listener {

    // Fragment实例
    private Fragment[] fragments;

    // Tabs图标
    private int[][] ICONS_RES;

    // Tabs标题
    private int[] tab_main_title;

    private TabContainerView mTabLayout;

    private ActionBar actionBar;
    private RelativeLayout rlConnecting;

    private FinDrawerLayout mDrawerLayout;

    private FeedBackFloatingView mFeedbackView;

    private ScreenShotListenManager mManager;

    private boolean isForeground = false;

    private IBadgeManager.OnBadgeCountUpdateListener mBadgeListener;

    private BroadcastReceiver mBR = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            recreate();
        }
    };

    // OkHttp Call, cancel when onDestroy().
    private Call mCall;

    private OkHttpClient mClient;

    private final static long DAY_MILL_SEC = 24 * 60 * 60 * 1000; // One day in mill-second.
    private final static String LAST_UPDATE_TS = "LAST_UPDATE_TS";
    private static final String FINO_VERSION_PREFS = "FINO_VERSION_PREFS";

    /**
     * 版本升级检查
     */
    private Callback mCallback = new Callback() {
        @Override
        public void onFailure(@NonNull Call call, @NonNull IOException e) {
        }

        @Override
        public void onResponse(Call call, @NonNull Response response) throws IOException {
            final int statusCode = response.code();
            if (call.isCanceled() || statusCode < 200 || statusCode >= 300) return;

            ResponseBody body = response.body();
            if (body == null) return;

            String bodyString = body.string();
            if (bodyString == null) return;

            VersionResp resp = (new Gson()).fromJson(bodyString, VersionResp.class);
            if (resp == null || resp.getData() == null || resp.getData().get(0) == null) return;

            Data data = resp.getData().get(0);

            if (!compareVersion(data.getVersion(), BuildConfig.VERSION_NAME)) return;

            String appName = getApplicationInfo().loadLabel(getPackageManager()).toString();
            AlertDialog.Builder builder = new AlertDialog
                    .Builder(HomeActivity.this)
                    .setTitle("\"" + appName + "\"发现了新版本");

            String remarks = data.getRemarks();
            if (!TextUtils.isEmpty(remarks)) {
                builder.setMessage(remarks);
            }

            builder.setPositiveButton("前去更新", (dialog, which) -> {
                Intent i = new Intent()
                        .setAction("android.intent.action.VIEW")
                        .setData(Uri.parse(data.getUrl()));
                HomeActivity.this.startActivity(i);

                getSharedPreferences(FINO_VERSION_PREFS, MODE_PRIVATE)
                        .edit()
                        .putLong(LAST_UPDATE_TS, System.currentTimeMillis())
                        .apply();
            });

            Boolean forceUpdate = data.getForceUpdate();
            if (forceUpdate == null || !forceUpdate) {
                builder.setNegativeButton("取消", (dialog, which) -> {
                    dialog.dismiss();
                    getSharedPreferences(FINO_VERSION_PREFS, MODE_PRIVATE)
                            .edit()
                            .putLong(LAST_UPDATE_TS, System.currentTimeMillis())
                            .apply();
                });
            } else {
                builder.setCancelable(false);
            }

            runOnUiThread(builder::show);
        }
    };

    private static int parseInt(String s) {
        // 本身是空
        if (TextUtils.isEmpty(s)) {
            return 0;
        }

        s = s.replaceAll("[^\\d]", "");

        // 裁剪之后字符串为空
        if (TextUtils.isEmpty(s)) {
            return 0;
        }

        // 裁剪之后不为空
        try {
            return Integer.parseInt(s);
        } catch (Exception e) {
            return 0;
        }
    }

    /**
     * 如果v1大于v2返回true，否则返回false
     */
    public static boolean compareVersion(String v1, String v2) {
        // ^([1-9]\d{0,1})(\.([1-9]\d{0,2})){1,3}$

        if (TextUtils.isEmpty(v1) || TextUtils.isEmpty(v2)) return false;

        String v1Trim_1 = StringsKt.substringBefore(v1, "-", v1);
        String v1Trim_2 = StringsKt.substringBefore(v1Trim_1, "_", v1Trim_1);
        String v2Trim_1 = StringsKt.substringBefore(v2, "-", v2);
        String v2Trim_2 = StringsKt.substringBefore(v2Trim_1, "_", v2Trim_1);

        final String[] str1 = v1Trim_2.split("\\.");
        final String[] str2 = v2Trim_2.split("\\.");

        if (str1.length == str2.length) {
            for (int i = 0; i < str1.length; i++) {
                final int i1 = parseInt(str1[i]);
                final int i2 = parseInt(str2[i]);
                if (i1 > i2) {
                    return true;
                } else if (i1 < i2) {
                    return false;
                }
            }
        } else {
            if (str1.length > str2.length) {
                for (int i = 0; i < str2.length; i++) {
                    final int i1 = parseInt(str1[i]);
                    final int i2 = parseInt(str2[i]);
                    if (i1 > i2) {
                        return true;
                    } else if (i1 < i2) {
                        return false;
                    } else {
                        if (str2.length == 1) continue;
                        if (i == str2.length - 1) {
                            for (int j = i; j < str1.length; j++) {
                                if (Integer.parseInt(str1[j]) != 0) {
                                    return true;
                                } else if (j == str1.length - 1) {
                                    return false;
                                }
                            }
                            return true;
                        }
                    }
                }
            } else {
                for (int i = 0; i < str1.length; i++) {
                    final int num1 = parseInt(str1[i]);
                    final int num2 = parseInt(str2[i]);
                    if (num1 > num2) {
                        return true;
                    } else if (num1 < num2) {
                        return false;
                    } else {
                        if (i == str1.length - 1) {
                            return false;
                        }
                    }
                }
            }
        }
        return false;
    }

    @Override
    public boolean slideBackDisable() {
        return true;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ThemeKit.INSTANCE.apply(this);
        setContentView(R.layout.activity_home);
        ForegroundCallbacks.get(this.getApplicationContext()).addListener(this);

        // Run on sub-Thread.
        Observable
                .fromCallable(() -> {
                    CrashReport.setUserId(
                            getApplicationContext(),
                            FinoChatClient.getInstance().accountManager().loginUserId());
                    return "";
                })
                .compose(this.bindToLifecycle())
                .subscribeOn(Schedulers.computation())
                .subscribe(s -> {
                }, throwable -> {
                });

        initViewData();
        initView();

        mBadgeListener = badgeManager -> {
            ShortcutBadger.applyCount(getApplicationContext(), badgeManager.getAllNoticeCount());
            int unReadMessageCount = badgeManager.getUnReadMessageCount();
            mTabLayout.setBadgeNumber(0, unReadMessageCount);
            mTabLayout.setBadgeNumber(1, badgeManager.getInviteRoomCount());
            mTabLayout.setBadgeNumber(2, badgeManager.getWorkBadgeCount());
            String title = getString(R.string.app_name);
            if (unReadMessageCount > 0) {
                actionBar.setTitle(title + "(" + unReadMessageCount + ")");
            } else {
                actionBar.setTitle(title);
            }
        };

        FinoChatClient.getInstance().getBadgeManager().addBadgeCountUpdateListener(mBadgeListener);
        LocalBroadcastManager.getInstance(this).registerReceiver(mBR, new IntentFilter("THEME_CHANGED"));

        initClient();
        versionCheck();
    }

    private void versionCheck() {
        // Cancel last one, start a new one.
        if (mCall != null) mCall.cancel();

        Disposable disposable = Observable
                .fromCallable(() -> {
                    long lastTs = getSharedPreferences(FINO_VERSION_PREFS, MODE_PRIVATE).getLong(LAST_UPDATE_TS, 0);
                    return System.currentTimeMillis() - lastTs > DAY_MILL_SEC;
                })
                .filter(aBoolean -> aBoolean) // can update.
                .map(aBoolean -> {
                    // String url = "http://111.230.173.185:8000/api/v1/finchat-control/updateApp/query?typeList=apk";
                    FinoChatOption option = ServiceFactory.getInstance().getOptions();
                    return new StrBuilder(option.getApiURLTrimmed())
                            .append(option.getApiPrefix())
                            .append("finchat-control/updateApp/query?typeList=apk")
                            .append("&jwt=")
                            .append(FinoChatClient.getInstance().getSessionManager().getCurrentSession().getCredentials().authorization)
                            .toString();
                })
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(s -> {
                    Request builder = new Request.Builder().url(s).build();
                    mCall = mClient.newCall(builder);
                    mCall.enqueue(mCallback);
                }, throwable -> Log.e("HomeActivity", "versionCheck", throwable));

        onDestroyDisposer.add(disposable);
    }

    private void initClient() {
        mClient = new OkHttpClient.Builder()
                .connectTimeout(10, TimeUnit.SECONDS)
                .readTimeout(10, TimeUnit.SECONDS).build();
    }

    private void initViewData() {
        // 普通版本
        fragments = new Fragment[]{
                FinoChatClient.getInstance().chatUIManager().conversationFragment(),
                FinoChatClient.getInstance().chatUIManager().contactFragment(),
//                new WorkFragment(),
                FinoChatClient.getInstance().chatUIManager().workFragment(),
                FinoChatClient.getInstance().chatUIManager().mineFragment()};

        ICONS_RES = new int[][]{
                {R.drawable.sdk_tapbar_ic_messages_normal, R.drawable.sdk_tapbar_ic_messages_selected},
                {R.drawable.sdk_tapbar_ic_contacts_normal, R.drawable.sdk_tapbar_ic_contacts_selected},
                {R.drawable.sdk_tapbar_ic_work_normal, R.drawable.sdk_tapbar_ic_work_selected},
                {R.drawable.sdk_tapbar_ic_me_normal, R.drawable.sdk_tapbar_ic_me_selected}};

        tab_main_title = new int[]{
                R.string.tab_main_title_message,
                R.string.tab_main_contact,
                R.string.tab_main_title_work,
                R.string.tab_main_title_me};
        Boolean isWork = FinoChatClient.getInstance().getOptions().getSettings().isWorkTab;
        if (Boolean.FALSE.equals(isWork)) {
            fragments = ArrayUtils.remove(fragments, 2);
            ICONS_RES = ArrayUtils.remove(ICONS_RES, 2);
            tab_main_title = ArrayUtils.remove(tab_main_title, 2);
        }
    }

    private FinoCallBack<Void> callBack;

    private void initView() {
        setSupportActionBar(findViewById(R.id.home_toolbar));
        actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(false);
        }

        rlConnecting = findViewById(R.id.rlConnecting);
        boolean isFeedback = FinoChatClient.getInstance().getOptions().appConfig.setting.feedback;
        if (isFeedback) {
            mFeedbackView = new FeedBackFloatingView(this);
            mManager = ScreenShotListenManager.newInstance(getApplicationContext());
            mManager.setListener(imagePath -> {
                if (FloatWindowPermissionHelper.INSTANCE.checkPermission(this)) {
                    if (isForeground) mFeedbackView.show(imagePath);
                }

                // 产品说如果系统没有权限，不需要提示用户获取，即不使用此功能
//                else {
//                    if (Build.VERSION.SDK_INT >= 23) {
//                        new AlertDialog.Builder(this)
//                                .setMessage("没有浮窗权限，请前往权限页面开启")
//                                .setTitle("权限提示")
//                                .setPositiveButton("开启", (dialog, which) -> {
//                                    Intent intent = new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION);
//                                    intent.setData(Uri.parse("package:" + getApplicationContext().getPackageName()));
//                                    intent.putExtra("imagePath", imagePath);
//                                    HomeActivity.this.startActivityForResult(intent, PERMISSION_ACTIVITY_RESULT);
//                                })
//                                .setNegativeButton("取消", (dialog, which) -> dialog.cancel())
//                                .create()
//                                .show();
//                    } else {
//                        new AlertDialog.Builder(this)
//                                .setMessage("没有浮窗权限，请前往权限页面开启")
//                                .setTitle("权限提示")
//                                .setPositiveButton("知道了", (dialog, which) -> {
//                                })
//                                .create()
//                                .show();
//                    }
//                }
            });
        }

        if (!FinoChatClient.getInstance().isSessionInitSuccess()) {
            showLoading(true);
            callBack = new SimpleCallBack<Void>() {
                @Override
                public void onSuccess(Void result) {
                    showLoading(false);
                }
            };
            FinoChatClient.getInstance().addSessionInitStatusObserver(callBack);
        } else {
            showLoading(false);
        }

        int[] TAB_COLORS = new int[]{
                ResourceKt.color(this, R.color.tab_text_color_normal),
                ResourceKt.attrColor(this, R.attr.TP_color_normal)};

        TabFragmentAdapter adapter = new TabFragmentAdapter(getSupportFragmentManager(), fragments);
        ViewPager pager = findViewById(R.id.home_view_pager);

        pager.setOffscreenPageLimit(2);
        pager.setAdapter(adapter);
        pager.addOnPageChangeListener(new ViewPager.OnPageChangeListener() {
            @Override
            public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {

            }

            @Override
            public void onPageSelected(int position) {
                if (FinoChatClient.getInstance().getLicenseService().getFeature().isApplet()) {
                    if (position == 0) {
                        mDrawerLayout.setDrawerLockMode(LOCK_MODE_UNLOCKED);
                    } else {
                        mDrawerLayout.setDrawerLockMode(LOCK_MODE_LOCKED_CLOSED);
                    }
                }
            }

            @Override
            public void onPageScrollStateChanged(int state) {

            }
        });
        mTabLayout = findViewById(R.id.tab_container);
        mTabLayout.initContainer(tab_main_title, ICONS_RES, TAB_COLORS);

        final int tab_icon = getResources().getDimensionPixelSize(R.dimen.tab_icon);
        mTabLayout.setContainerLayout(R.layout.part_tab_container, R.id.iv_tab_icon,
                R.id.tv_tab_badge, R.id.tv_tab_text, tab_icon, tab_icon);
        mTabLayout.setViewPager(pager);
        mTabLayout.setEventListener(this);
        pager.setCurrentItem(getIntent().getIntExtra("tab", 0));

        // 左侧抽屉
        mDrawerLayout = findViewById(R.id.drawer_layout);
        FrameLayout mDrawerViewContainer = findViewById(R.id.fl_drawer_view_container);
        if (FinoChatClient.getInstance().getLicenseService().getFeature().isApplet()) {
            mDrawerLayout.setDrawerLockMode(LOCK_MODE_UNLOCKED);
            FinoChatClient.getInstance().chatUIManager()
                    .inflateAppsDrawerView(mDrawerLayout, mDrawerViewContainer);
        } else {
            mDrawerLayout.setDrawerLockMode(LOCK_MODE_LOCKED_CLOSED);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        // https://github.com/vector-im/vector-android/issues/323
        // the tool bar color is not restored on some devices.
        // mToolbar.setBackgroundResource(R.color.color_fafafa);
        startListenNetworkStatus();

        // 反馈建议
        if (mManager != null) {
            mManager.startListen();
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        stopListenNetworkStatus();

        // 反馈建议
        if (mManager != null) {
            mManager.stopListen();
        }
    }

    @Override
    protected void onDestroy() {
        LocalBroadcastManager.getInstance(this).unregisterReceiver(mBR);

        ForegroundCallbacks.get(this.getApplicationContext()).removeListener(this);
        mTabLayout.setViewPager(null);
        ((ViewPager) findViewById(R.id.home_view_pager)).setAdapter(null);
        mTabLayout.setEventListener(null);

        FinoChatClient.getInstance().removeSessionInitStatusObserver(callBack);
        callBack = null;

        FinoChatClient.getInstance().getBadgeManager().removeBadgeCountUpdateListener(mBadgeListener);
        mBadgeListener = null;

        MemoryLeakUtil.INSTANCE.fix(this);

        if (mCall != null) mCall.cancel();

        super.onDestroy();
    }

    private void startListenNetworkStatus() {
        FinoChatClient.getInstance().networkManager().addNetworkEventListener(this);
    }

    private void stopListenNetworkStatus() {
        FinoChatClient.getInstance().networkManager().removeNetworkEventListener(this);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.home_toolbar_menu, menu);
        MenuItem itemDrawer = menu.findItem(R.id.home_toolbar_menu_item_drawer);
        if (itemDrawer != null) {
            itemDrawer.setVisible(FinoChatClient.getInstance().getLicenseService().getFeature().isApplet());
        }
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        switch (item.getItemId()) {
            case R.id.home_toolbar_menu_item_drawer:
                openDrawer();
                break;
            // 发起聊天、创建群聊
            case R.id.home_toolbar_menu_item_create_chat:
                FinoChatClient.getInstance()
                        .chatUIManager()
                        .createHomeChatPopupWindow(this,
                                R.id.home_toolbar_menu_item_create_chat);
                break;
            // 跳转到搜索页面
            case R.id.home_toolbar_menu_item_search:
                FinoChatClient.getInstance().chatUIManager().startSearch(this);
                break;
            default:
                break;
        }
        return super.onOptionsItemSelected(item);
    }

    /**
     * 打开抽屉
     */
    private void openDrawer() {
        if (mDrawerLayout != null) {
            mDrawerLayout.openDrawer(GravityCompat.START);
        }
    }

    @Override
    public void onNetworkConnectionUpdate(boolean isConnected) {
    }

    @Override
    public void onTabDoubleClicked(int tabIndex) {
        if (tabIndex == 0) {
            FinoChatClient.getInstance().chatUIManager().locateToNextChatWithUnreadMessages(fragments[0]);
        }
    }

    /**
     * 显示/隐藏Loading View
     */
    private void showLoading(boolean isLoading) {
        if (isLoading) {
            rlConnecting.setVisibility(View.VISIBLE);
        } else {
            rlConnecting.setVisibility(View.GONE);
        }
    }

    @Override
    public void onBecameForeground() {
        isForeground = true;
    }

    @Override
    public void onBecameBackground() {
        isForeground = false;
    }
}