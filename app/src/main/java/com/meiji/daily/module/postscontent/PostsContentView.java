package com.meiji.daily.module.postscontent;

import android.annotation.SuppressLint;
import android.arch.lifecycle.Observer;
import android.content.Intent;
import android.content.res.ColorStateList;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.design.widget.CollapsingToolbarLayout;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v4.widget.NestedScrollView;
import android.support.v7.widget.Toolbar;
import android.text.TextUtils;
import android.view.KeyEvent;
import android.view.View;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.ImageView;

import com.afollestad.materialdialogs.MaterialDialog;
import com.afollestad.materialdialogs.Theme;
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions;
import com.meiji.daily.App;
import com.meiji.daily.GlideApp;
import com.meiji.daily.R;
import com.meiji.daily.data.remote.IApi;
import com.meiji.daily.module.base.BaseFragment;
import com.meiji.daily.util.SettingHelper;

import javax.inject.Inject;
import javax.inject.Named;

/**
 * Created by Meiji on 2017/12/6.
 */

public class PostsContentView extends BaseFragment {

    static final String ARGUMENT_TITLEIMAGE = "ARGUMENT_TITLEIMAGE";
    static final String ARGUMENT_TITLE = "ARGUMENT_TITLE";
    static final String ARGUMENT_SLUG = "ARGUMENT_SLUG";

    @Inject
    @Named("image")
    String mImage;
    @Inject
    @Named("title")
    String mTitle;
    @Inject
    @Named("slug")
    int mSlug;
    @Inject
    PostsContentViewModel mModel;
    @Inject
    SettingHelper mSettingHelper;

    private WebView mWebView;
    private MaterialDialog mDialog;

    public static PostsContentView newInstance(String titleImage, String title, int slug) {
        Bundle args = new Bundle();
        args.putString(ARGUMENT_TITLEIMAGE, titleImage);
        args.putString(ARGUMENT_TITLE, title);
        args.putInt(ARGUMENT_SLUG, slug);
        PostsContentView fragment = new PostsContentView();
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    protected void initInject() {
        DaggerPostsContentComponent.builder()
                .appComponent(App.sAppComponent)
                .postsContentModule(new PostsContentModule(this))
                .build().inject(this);
    }

    public void onSetWebView(String url) {
        mWebView.loadDataWithBaseURL(null, url, "text/html", "utf-8", null);
    }

    @SuppressLint("SetJavaScriptEnabled")
    private void initWebClient() {
        WebSettings settings = mWebView.getSettings();
        settings.setJavaScriptEnabled(true);
        // 缩放,设置为不能缩放可以防止页面上出现放大和缩小的图标
        settings.setBuiltInZoomControls(false);
        // 缓存
        settings.setCacheMode(WebSettings.LOAD_DEFAULT);
        // 开启DOM storage API功能
        settings.setDomStorageEnabled(true);
        // 开启application Cache功能
        settings.setAppCacheEnabled(false);
        // 不调用第三方浏览器即可进行页面反应
        mWebView.setWebViewClient(new WebViewClient() {
            @Override
            public boolean shouldOverrideUrlLoading(WebView view, String url) {
                view.loadUrl(url);
                return true;
            }
        });

        mWebView.setOnKeyListener(new View.OnKeyListener() {
            @Override
            public boolean onKey(View view, int i, KeyEvent keyEvent) {
                if ((keyEvent.getKeyCode() == KeyEvent.KEYCODE_BACK) && mWebView.canGoBack()) {
                    mWebView.goBack();
                    return true;
                }
                return false;
            }
        });
    }

    @Override
    protected int attachLayoutId() {
        return R.layout.activity_postscontent;
    }

    @Override
    protected void initViews(View view) {
        ImageView ivHeader = view.findViewById(R.id.iv_titleimage);
        Toolbar toolbar_title = view.findViewById(R.id.toolbar_title);
        mWebView = view.findViewById(R.id.webview_content);
        FloatingActionButton fab_share = view.findViewById(R.id.fab_share);
        CollapsingToolbarLayout collapsingToolbarLayout = view.findViewById(R.id.collapsing_layout);
        final NestedScrollView scrollView = view.findViewById(R.id.scrollView);

        initToolBar(toolbar_title, true, null);

        toolbar_title.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                scrollView.smoothScrollTo(0, 0);
            }
        });

        fab_share.setBackgroundTintList(ColorStateList.valueOf(mSettingHelper.getColor()));
        fab_share.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent shareIntent = new Intent()
                        .setAction(Intent.ACTION_SEND)
                        .setType("text/plain");
                String shareText = mTitle + " " + IApi.POST_URL + mSlug;
                shareIntent.putExtra(Intent.EXTRA_TEXT, shareText);
                startActivity(Intent.createChooser(shareIntent, getString(R.string.share_to)));
            }
        });

        collapsingToolbarLayout.setExpandedTitleTextAppearance(R.style.ExpandedAppBar);
        collapsingToolbarLayout.setCollapsedTitleTextAppearance(R.style.CollapsedAppBar);
        collapsingToolbarLayout.setTitle(mTitle);

        mDialog = new MaterialDialog.Builder(getContext())
                .progress(true, 0)
                .content(R.string.md_loading)
                .theme(mSettingHelper.getIsNightMode() ? Theme.DARK : Theme.LIGHT)
                .cancelable(true)
                .build();

        if (TextUtils.isEmpty(mImage)) {
            ivHeader.setImageResource(R.drawable.error_image);
            ivHeader.setScaleType(ImageView.ScaleType.CENTER_CROP);
        } else {
            GlideApp.with(this)
                    .load(mImage)
                    .centerCrop()
                    .error(R.color.viewBackground)
                    .transition(new DrawableTransitionOptions().crossFade())
                    .into(ivHeader);
        }

        initWebClient();
    }

    @Override
    protected void subscribeUI() {
        mModel.getHTML().observe(this, new Observer<String>() {
            @Override
            public void onChanged(@Nullable String s) {
                if (!TextUtils.isEmpty(s)) {
                    onSetWebView(s);
                } else {
                    onShowNetError();
                }
            }
        });
        mModel.isLoading().observe(this, new Observer<Boolean>() {
            @Override
            public void onChanged(@Nullable Boolean aBoolean) {
                if (aBoolean) {
                    onShowLoading();
                } else {
                    onHideLoading();
                }
            }
        });
    }

    public void onShowLoading() {
        mDialog.show();
    }

    public void onHideLoading() {
        mDialog.dismiss();
    }

    public void onShowNetError() {
        mDialog.dismiss();
        Snackbar.make(mWebView, R.string.network_error, Snackbar.LENGTH_SHORT).show();
    }
}
