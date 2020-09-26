package com.finogeeks.finochatapp.views;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Canvas;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.viewpager.widget.PagerAdapter;
import androidx.viewpager.widget.ViewPager;

import com.finogeeks.finochatapp.R;
import com.finogeeks.finochatapp.utils.ViewExtKt;
import com.finogeeks.utility.views.BadgeView;

public class TabContainerView extends LinearLayout {

    private ViewPager mViewPager;

    /**
     * 默认颜色
     */
    private int mTextNormalColor;

    /**
     * 选中时颜色值
     */
    private int mTextSelectedColor;

    /**
     * 前一次选择位置
     */
    private int mLastPosition;

    /**
     * 当前选中位置
     */
    private int mSelectedPosition;

    /**
     * 选择偏移位置
     */
    private float mSelectionOffset;

    /**
     * tab 标题
     */
    private int[] mTitles;

    /**
     * tab icon集合
     */
    private int[][] mIconRes;

    /**
     * tab item 视图集合
     */
    private View[] mTabViews;

    /**
     * 布局文件id
     */
    private int mLayoutId;

    /**
     * textView 控件id
     */
    private int mTextViewId;

    /**
     * BadgeView 控件id
     */
    private int mBadgeViewId;

    /**
     * icon 控件id
     */
    private int mIconViewId;

    /**
     * icon宽度
     */
    private int mIconWidth;

    /**
     * icon高度
     */
    private int mIconHeight;

    /**
     * 是否显示过渡颜色效果
     */
    private static final boolean mShowTransitionColor = true;

    /**
     * {@link EventListener}对象
     */
    private EventListener mListener;

    public TabContainerView(Context context) {
        this(context, null);
    }

    public TabContainerView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public void initContainer(int[] titles, int[][] iconsRes, int[] colors) {
        this.mTitles = titles;
        this.mIconRes = iconsRes;
        this.mTextNormalColor = colors[0];
        this.mTextSelectedColor = colors[1];
    }

    /**
     * 设置布局文件及相关控件id
     *
     * @param layout  layout布局文件 id
     * @param iconId  ImageView 控件 id id <= 0 时不显示
     * @param badgeId BadgeView 控件 id id <= 0 时不显示
     * @param textId  TextView 控件 id id <= 0 时不显示
     * @param width   icon 宽度
     * @param height  icon 高度
     */
    public void setContainerLayout(int layout, int iconId, int badgeId, int textId, int width, int height) {
        mLayoutId = layout;
        mTextViewId = textId;
        mBadgeViewId = badgeId;
        mIconViewId = iconId;
        mIconWidth = width;
        mIconHeight = height;
    }

    public void setViewPager(ViewPager viewPager) {
        removeAllViews();
        mViewPager = viewPager;
        if (viewPager != null && viewPager.getAdapter() != null) {
            viewPager.addOnPageChangeListener(new InternalViewPagerListener());
            addTabViewToContainer();
        }
    }

    /**
     * <p>添加tab view到当前容器</p>
     */
    private void addTabViewToContainer() {
        final PagerAdapter adapter = mViewPager.getAdapter();
        if (adapter == null) {
            return;
        }

        mTabViews = new View[adapter.getCount()];

        for (int index = 0, len = adapter.getCount(); index < len; index++) {

            final View tabView = LayoutInflater.from(getContext()).inflate(mLayoutId, this, false);
            mTabViews[index] = tabView;

            /*tabIconView初始化*/
            TabIconView iconView = null;
            if (mIconViewId > 0) {
                iconView = tabView.findViewById(mIconViewId);
                iconView.init(mIconRes[index][0], mIconRes[index][1], mIconWidth, mIconHeight);
            }

            /*BadgeView初始化*/
            BadgeView badgeView;
            if (mBadgeViewId > 0) {
                badgeView = tabView.findViewById(mBadgeViewId);
                badgeView.setVisibility(View.GONE);
            }

            /*tabTextView初始化*/
            TextView textView = null;
            if (mTextViewId > 0) {
                textView = tabView.findViewById(mTextViewId);
                textView.setText(getResources().getString(mTitles[index]));
            }

            /*设置宽度，等分container*/
            LayoutParams lp = (LayoutParams) tabView.getLayoutParams();
            lp.width = 0;
            lp.weight = 1;

            /*添加tab点击事件*/
            addOnTabClicksListener(index);

            /*设置当前状态*/
            if (index == mViewPager.getCurrentItem()) {
                if (iconView != null) {
                    iconView.offsetChanged(0);
                }
                tabView.setSelected(true);
                if (textView != null) {
                    textView.setTextColor(mTextSelectedColor);
                }
            }

            addView(tabView);
        }
    }

    /**
     * <p>viewPager滑动改变监听事件</p>
     */
    private class InternalViewPagerListener implements ViewPager.OnPageChangeListener {
//        private int mScrollState;

        @Override
        public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {
            onViewPagerPageChanged(position, positionOffset);
        }

        @Override
        public void onPageSelected(int position) {
            for (int i = 0; i < getChildCount(); i++) {
                if (mIconViewId > 0) {
                    ((TabIconView) mTabViews[i].findViewById(mIconViewId)).offsetChanged(position == i ? 0 : 1);
                }
                if (mTextViewId > 0) {
                    ((TextView) mTabViews[i].findViewById(mTextViewId)).setTextColor(position == i ? mTextSelectedColor : mTextNormalColor);
                }
            }

//            if (mScrollState == ViewPager.SCROLL_STATE_IDLE) {
//                onViewPagerPageChanged(position, 0f);
//            }

            for (int i = 0, size = getChildCount(); i < size; i++) {
                getChildAt(i).setSelected(position == i);
            }
        }

        @Override
        public void onPageScrollStateChanged(int state) {
//            mScrollState = state;
        }
    }

    /**
     * viewpager滑动改变后更新当前container视图
     *
     * @param position       当前选择position
     * @param positionOffset position 偏移量
     */
    private void onViewPagerPageChanged(int position, float positionOffset) {
        mSelectedPosition = position;
        mSelectionOffset = positionOffset;
        if (positionOffset == 0f && mLastPosition != mSelectedPosition) {
            mLastPosition = mSelectedPosition;
        }
        invalidate();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        if (getChildCount() > 0) {
            /*当发生偏移时，绘制渐变区域*/
            if (mSelectionOffset > 0f && mSelectedPosition < (getChildCount() - 1) && mShowTransitionColor) {

                /*获取当前tab和下一tab view */
                View selectedTab = getChildAt(mSelectedPosition);
                View nextTab = getChildAt(mSelectedPosition + 1);

                /*显示tab icon时,刷新各自view 透明度*/
                if (mIconViewId > 0) {
                    View selectedIconView = selectedTab.findViewById(mIconViewId);
                    View nextIconView = nextTab.findViewById(mIconViewId);

                    //draw icon alpha
                    if (selectedIconView instanceof TabIconView && nextIconView instanceof TabIconView) {
                        ((TabIconView) selectedIconView).offsetChanged(mSelectionOffset);
                        ((TabIconView) nextIconView).offsetChanged(1 - mSelectionOffset);
                    }
                }

                /*显示tab text,刷新各自view 透明度*/
                if (mTextViewId > 0) {
                    View selectedTextView = selectedTab.findViewById(mTextViewId);
                    View nextTextView = nextTab.findViewById(mTextViewId);

                    //draw text color
                    int selectedColor = evaluate(mSelectionOffset, mTextSelectedColor, mTextNormalColor);
                    int nextColor = evaluate(1 - mSelectionOffset, mTextSelectedColor, mTextNormalColor);

                    if (selectedTextView instanceof TextView && nextTextView instanceof TextView) {
                        ((TextView) selectedTextView).setTextColor(selectedColor);
                        ((TextView) nextTextView).setTextColor(nextColor);
                    }
                }
            }
        }
    }

    /**
     * Tab item 点击事件，包括单击事件和非单击事件
     *
     * @param index The index of the tab in {@link #mTabViews}
     */
    @SuppressLint("CheckResult")
    public void addOnTabClicksListener(final int index) {
        ViewExtKt.clicks(mTabViews[index])
                .subscribe(i -> {
                    if (i > 1) {
                        if (mListener != null) {
                            mListener.onTabDoubleClicked(index);
                        }
                    } else if (i > 0) {
                        mViewPager.setCurrentItem(index, false);
                    }
                });
    }

//    public void setOnPageChangeListener(ViewPager.OnPageChangeListener listener) {
//        mViewPagerPageChangeListener = listener;
//    }

    /**
     * This function returns the calculated in-between value for a color
     * given integers that represent the start and end values in the four
     * bytes of the 32-bit int. Each channel is separately linearly interpolated
     * and the resulting calculated values are recombined into the return value.
     *
     * @param fraction   The fraction from the starting to the ending values
     * @param startValue A 32-bit int value representing colors in the
     *                   separate bytes of the parameter
     * @param endValue   A 32-bit int value representing colors in the
     *                   separate bytes of the parameter
     * @return A value that is calculated to be the linearly interpolated
     * result, derived by separating the start and end values into separate
     * color channels and interpolating each one separately, recombining the
     * resulting values in the same way.
     */
    public int evaluate(float fraction, Object startValue, Object endValue) {
        int startInt = (Integer) startValue;
        int startA = (startInt >> 24) & 0xff;
        int startR = (startInt >> 16) & 0xff;
        int startG = (startInt >> 8) & 0xff;
        int startB = startInt & 0xff;

        int endInt = (Integer) endValue;
        int endA = (endInt >> 24) & 0xff;
        int endR = (endInt >> 16) & 0xff;
        int endG = (endInt >> 8) & 0xff;
        int endB = endInt & 0xff;

        return (startA + (int) (fraction * (endA - startA))) << 24 |
                (startR + (int) (fraction * (endR - startR))) << 16 |
                (startG + (int) (fraction * (endG - startG))) << 8 |
                (startB + (int) (fraction * (endB - startB)));
    }

    /**
     * Set the number displayed in the BadgeView.
     *
     * @param tabIndex the index of the tab.
     * @param number   the number displayed in the BadgeView.
     */
    public void setBadgeNumber(int tabIndex, int number) {
        BadgeView badgeView = mTabViews[tabIndex].findViewById(R.id.tv_tab_badge);
        if (badgeView != null) {
            badgeView.setNumber(number);
        }
    }

    public void setEventListener(EventListener listener) {
        mListener = listener;
    }

    public interface EventListener {

        /**
         * Invoked when a tab was double clicked.
         *
         * @param tabIndex the index of the tab in {@link #mTabViews}
         */
        void onTabDoubleClicked(int tabIndex);
    }
}