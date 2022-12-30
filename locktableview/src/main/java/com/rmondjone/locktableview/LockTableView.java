package com.rmondjone.locktableview;

import android.content.Context;

import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;

import android.text.Layout;
import android.text.StaticLayout;
import android.text.TextPaint;
import android.util.Log;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.HorizontalScrollView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;


import com.rmondjone.xrecyclerview.ProgressStyle;
import com.rmondjone.xrecyclerview.XRecyclerView;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

/**
 * 说明 可锁定首行和首列的表格视图
 * 作者 郭翰林
 * 创建时间 2017/3/29.
 */

public class LockTableView {
    /**
     * 上下文
     */
    private Context mContext;
    /**
     * 表格父视图
     */
    private ViewGroup mContentView;
    /**
     * 表格数据，每一行为一条数据，从表头计算
     */
    private ArrayList<ArrayList<String>> mTableDatas = new ArrayList<ArrayList<String>>();
    /**
     * 表格视图
     */
    private View mTableView;
    /**
     * 是否锁定首行
     */
    private boolean isLockFristRow = true;
    /**
     * 是否锁定首列
     */
    private boolean isLockFristColumn = true;
    /**
     * 最大列宽(dp)
     */
    private int maxColumnWidth;
    /**
     * 最小列宽(dp)
     */
    private int minColumnWidth;
    /**
     * 最大行高(dp)
     */
    private int maxRowHeight;
    /**
     * 最小行高dp)
     */
    private int minRowHeight;
    /**
     * 第一行背景颜色
     */
    private int mFristRowBackGroudColor;
    /**
     * 数据为空时的缺省值
     */
    private String mNullableString;
    /**
     * 单元格字体大小
     */
    private int mTextViewSize;
    /**
     * 表格头部字体颜色
     */
    private int mTableHeadTextColor;
    /**
     * 表格内容字体颜色
     */
    private int mTableContentTextColor;
    /**
     * 表格横向滚动监听事件
     */
    private OnTableViewListener mTableViewListener;

    /**
     * 表格横向滚动到边界监听事件
     */
    private OnTableViewRangeListener mTableViewRangeListener;

    /**
     * 表格上拉刷新、下拉加载监听事件
     */
    private OnLoadingListener mOnLoadingListener;
    /**
     * Item点击事件
     */
    private OnItemClickListenter mOnItemClickListenter;
    /**
     * Item长按事件
     */
    private OnItemLongClickListenter mOnItemLongClickListenter;
    /**
     * Item选中样式
     */
    private int mOnItemSeletor;
    /**
     * 单元格内边距
     */
    private int mCellPadding;
    /**
     * 要改变的列集合
     */
    private HashMap<Integer, Integer> mChangeColumns = new HashMap<>();


    //表格数据
    /**
     * 表格第一行数据,不包括第一个元素
     */
    private ArrayList<String> mTableFristData = new ArrayList<>();
    /**
     * 表格第一列数据，不包括第一个元素
     */
    private ArrayList<String> mTableColumnDatas = new ArrayList<>();
    /**
     * 表格左上角数据
     */
    private String mColumnTitle;
    /**
     * 表格每一行数据，不包括第一行和第一列
     */
    private ArrayList<ArrayList<String>> mTableRowDatas = new ArrayList<ArrayList<String>>();
    /**
     * 记录每列最大宽度
     */
    private ArrayList<Integer> mColumnMaxWidths = new ArrayList<Integer>();
    /**
     * 记录每行最大高度
     */
    private ArrayList<Integer> mRowMaxHeights = new ArrayList<Integer>();
    /**
     * 把所有的滚动视图放图列表，后面实现联动效果
     */
    private ArrayList<HorizontalScrollView> mScrollViews = new ArrayList<HorizontalScrollView>();


    //表格视图
    /**
     * 表格左上角视图
     */
    private TextView mColumnTitleView;
    /**
     * 第一行布局（锁状态）
     */
    private LinearLayout mLockHeadView;
    /**
     * 第一行布局（未锁状态）
     */
    private LinearLayout mUnLockHeadView;
    /**
     * 第一行滚动视图（锁状态）
     */
    private CustomHorizontalScrollView mLockScrollView;
    /**
     * 第一行滚动视图（未锁状态）
     */
    private CustomHorizontalScrollView mUnLockScrollView;
    /**
     * 表格主视图
     */
    private XRecyclerView mTableScrollView;
    /**
     * 列表适配器
     */
    private TableViewAdapter mTableViewAdapter;


    /**
     * 构造方法
     *
     * @param mContext     上下文
     * @param mContentView 表格父视图
     * @param mTableDatas  表格数据
     */
    public LockTableView(Context mContext, ViewGroup mContentView, ArrayList<ArrayList<String>> mTableDatas) {
        this.mContext = mContext;
        this.mContentView = mContentView;
        this.mTableDatas = mTableDatas;
        initAttrs();
    }

    /**
     * 初始化属性
     */
    private void initAttrs() {
        mTableView = LayoutInflater.from(mContext).inflate(R.layout.locktableview, null);
        maxColumnWidth = 100;
        minColumnWidth = 70;
        minRowHeight = 20;
        maxRowHeight = 60;
        mNullableString = "N/A";
        mTableHeadTextColor = R.color.beijin;
        mTableContentTextColor = R.color.border_color;
        mFristRowBackGroudColor = R.color.table_head;
        mTextViewSize = 16;
        mCellPadding=DisplayUtil.dip2px(mContext,45);
    }

    /**
     * 展现视图
     */
    public void show() {
        initData();
        initView();
        mContentView.removeAllViews();//清空视图
        mContentView.addView(mTableView);
    }


    /**
     * 初始化表格数据
     */
    private void initData() {
        if (mTableDatas != null && mTableDatas.size() > 0) {
            //检查数据，如果有一行数据长度不一致，以最长为标准填"N/A"字符串,如果有null也替换
            int maxLength = 0;
            for (int i = 0; i < mTableDatas.size(); i++) {
                if (mTableDatas.get(i).size() >= maxLength) {
                    maxLength = mTableDatas.get(i).size();
                }
                ArrayList<String> rowDatas = mTableDatas.get(i);
                for (int j = 0; j < rowDatas.size(); j++) {
                    if (rowDatas.get(j) == null || rowDatas.get(j).equals("")) {
                        rowDatas.set(j, mNullableString);
                    }
                }
                mTableDatas.set(i, rowDatas);
            }
//            Log.e("每行最多个数",maxLength+"");
            for (int i = 0; i < mTableDatas.size(); i++) {
                ArrayList<String> rowDatas = mTableDatas.get(i);
                if (rowDatas.size() < maxLength) {
                    int size = maxLength - rowDatas.size();
                    for (int j = 0; j < size; j++) {
                        rowDatas.add(mNullableString);
                    }
                    mTableDatas.set(i, rowDatas);
                }
            }

//            //测试
//            for (int i=0;i<mTableDatas.size();i++){
//                ArrayList<String> rowDatas=mTableDatas.get(i);
//                StringBuffer b=new StringBuffer();
//                for (String str:rowDatas){
//                    b.append("["+str+"]");
//                }
//                Log.e("第"+i+"行数据",b.toString()+"/"+rowDatas.size()+"个");
//            }
            //初始化每列最大宽度
            for (int i = 0; i < mTableDatas.size(); i++) {
                ArrayList<String> rowDatas = mTableDatas.get(i);
                StringBuffer buffer = new StringBuffer();
                for (int j = 0; j < rowDatas.size(); j++) {
                    TextView textView = new TextView(mContext);
                    textView.setTextSize(TypedValue.COMPLEX_UNIT_SP, mTextViewSize);
                    textView.setText(rowDatas.get(j));
                    textView.setGravity(Gravity.CENTER);
                    //设置布局
                    LinearLayout.LayoutParams textViewParams = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT,
                            LinearLayout.LayoutParams.WRAP_CONTENT);
                    textViewParams.setMargins(mCellPadding, mCellPadding, mCellPadding, mCellPadding);//android:layout_margin="15dp"
                    textView.setLayoutParams(textViewParams);
                    if (i == 0) {
                        mColumnMaxWidths.add(measureTextWidth(textView, rowDatas.get(j)));
                        buffer.append("[" + measureTextWidth(textView, rowDatas.get(j)) + "]");
                    } else {
                        int length = mColumnMaxWidths.get(j);
                        int current = measureTextWidth(textView, rowDatas.get(j));
                        if (current > length) {
                            mColumnMaxWidths.set(j, current);
                        }
                        buffer.append("[" + measureTextWidth(textView, rowDatas.get(j)) + "]");
                    }
                }
//                Log.e("第"+i+"行列最大宽度",buffer.toString());
            }
            //如果用户指定某列宽度则按照用户指定宽度算
            if (mChangeColumns.size() > 0) {
                for (Integer key : mChangeColumns.keySet()) {
                    changeColumnWidth(key, mChangeColumns.get(key));
                }
            }
//            Log.e("每列最大宽度dp:",mColumnMaxWidths.toString());


            //初始化每行最大高度
            for (int i = 0; i < mTableDatas.size(); i++) {
                ArrayList<String> rowDatas = mTableDatas.get(i);
                StringBuffer buffer = new StringBuffer();

                TextView textView = new TextView(mContext);
                textView.setTextSize(TypedValue.COMPLEX_UNIT_SP, mTextViewSize);
                textView.setGravity(Gravity.CENTER);
                //设置布局
                LinearLayout.LayoutParams textViewParams = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT);
                textViewParams.setMargins(mCellPadding, mCellPadding, mCellPadding, mCellPadding);//android:layout_margin="15dp"
                textView.setLayoutParams(textViewParams);
                int maxHeight = measureTextHeight(textView, rowDatas.get(0));
                mRowMaxHeights.add(maxHeight);
                for (int j = 0; j < rowDatas.size(); j++) {
                    int currentHeight;
                    //如果用户指定某列宽度则按照用户指定宽度算对应列的高度
                    if (mChangeColumns.size() > 0 && mChangeColumns.containsKey(j)) {
                        currentHeight = getTextViewHeight(textView, rowDatas.get(j), mChangeColumns.get(j));
                    } else {
                        currentHeight = measureTextHeight(textView, rowDatas.get(j));
                    }
                    buffer.append("[" + currentHeight + "]");
                    if (currentHeight > maxHeight) {
                        mRowMaxHeights.set(i, currentHeight);
                    }
                }
//                Log.e("第"+i+"行高度",buffer.toString());
            }
//            Log.e("每行最大高度dp:",mRowMaxHeights.toString());

            if (isLockFristRow) {
                ArrayList<String> fristRowDatas = (ArrayList<String>) mTableDatas.get(0).clone();
                if (isLockFristColumn) {
                    //锁定第一列
                    mColumnTitle = fristRowDatas.get(0);
                    fristRowDatas.remove(0);
                    mTableFristData.addAll(fristRowDatas);
                    //构造第一列数据,并且构造表格每行数据
                    for (int i = 1; i < mTableDatas.size(); i++) {
                        ArrayList<String> rowDatas = (ArrayList<String>) mTableDatas.get(i).clone();
                        mTableColumnDatas.add(rowDatas.get(0));
                        rowDatas.remove(0);
                        mTableRowDatas.add(rowDatas);
                    }
                } else {
                    mTableFristData.addAll(fristRowDatas);
                    for (int i = 1; i < mTableDatas.size(); i++) {
                        mTableRowDatas.add(mTableDatas.get(i));
                    }
                }
            } else {
                if (isLockFristColumn) {
                    //锁定第一列
                    //构造第一列数据,并且构造表格每行数据
                    for (int i = 0; i < mTableDatas.size(); i++) {
                        ArrayList<String> rowDatas = (ArrayList<String>) mTableDatas.get(i).clone();
                        mTableColumnDatas.add(rowDatas.get(0));
                        rowDatas.remove(0);
                        mTableRowDatas.add(rowDatas);
                    }
                } else {
                    for (int i = 0; i < mTableDatas.size(); i++) {
                        mTableRowDatas.add(mTableDatas.get(i));
                    }
                }
            }
//            Log.e("第一行数据", mTableFristData.toString());
//            Log.e("第一列数据", mTableColumnDatas.toString());
//            Log.e("每行数据", mTableRowDatas.toString());
        } else {
            Toast.makeText(mContext, "表格数据为空！", Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * 初始化表格视图
     */
    private void initView() {
        mColumnTitleView = (TextView) mTableView.findViewById(R.id.lockHeadView_Text);
        mLockHeadView = (LinearLayout) mTableView.findViewById(R.id.lockHeadView);
        mUnLockHeadView = (LinearLayout) mTableView.findViewById(R.id.unLockHeadView);
        mLockScrollView = (CustomHorizontalScrollView) mTableView.findViewById(R.id.lockHeadView_ScrollView);
        mUnLockScrollView = (CustomHorizontalScrollView) mTableView.findViewById(R.id.unlockHeadView_ScrollView);
        //表格主视图
        mTableScrollView = (XRecyclerView) mTableView.findViewById(R.id.table_scrollView);
        LinearLayoutManager layoutManager = new LinearLayoutManager(mContext);
        layoutManager.setOrientation(LinearLayoutManager.VERTICAL);
        mTableScrollView.setLayoutManager(layoutManager);
        mTableScrollView.setArrowImageView(R.drawable.iconfont_downgrey);
        mTableScrollView.setRefreshProgressStyle(ProgressStyle.BallRotate);
        mTableScrollView.setLoadingMoreProgressStyle(ProgressStyle.BallRotate);
        mTableScrollView.setLoadingListener(new XRecyclerView.LoadingListener() {
            @Override
            public void onRefresh() {
                if (mOnLoadingListener != null) {
                    mOnLoadingListener.onRefresh(mTableScrollView, mTableDatas);
                }
            }

            @Override
            public void onLoadMore() {
                if (mOnLoadingListener != null) {
                    mOnLoadingListener.onLoadMore(mTableScrollView, mTableDatas);
                }
            }
        });
        mTableViewAdapter = new TableViewAdapter(mContext, mTableColumnDatas, mTableRowDatas, isLockFristColumn, isLockFristRow);
        mTableViewAdapter.setCellPadding(mCellPadding);
        mTableViewAdapter.setColumnMaxWidths(mColumnMaxWidths);
        mTableViewAdapter.setRowMaxHeights(mRowMaxHeights);
        mTableViewAdapter.setTextViewSize(mTextViewSize);
        mTableViewAdapter.setTableContentTextColor(mTableContentTextColor);
        mTableViewAdapter.setTableHeadTextColor(mTableHeadTextColor);
        mTableViewAdapter.setFristRowBackGroudColor(mFristRowBackGroudColor);
        mTableViewAdapter.setHorizontalScrollView(new OnTableViewListener() {
            @Override
            public void onTableViewScrollChange(int x, int y) {
                changeAllScrollView(x, y);
            }
        });
        if (mOnItemClickListenter != null) {
            mTableViewAdapter.setOnItemClickListenter(mOnItemClickListenter);
        }
        if (mOnItemLongClickListenter != null) {
            mTableViewAdapter.setOnItemLongClickListenter(mOnItemLongClickListenter);
        }
        if (mOnItemSeletor != 0) {
            mTableViewAdapter.setOnItemSeletor(mOnItemSeletor);
        } else {
            mTableViewAdapter.setOnItemSeletor(R.color.dashline_color);
        }
        mTableViewAdapter.setTableViewRangeListener(new OnTableViewRangeListener() {
            @Override
            public void onLeft(HorizontalScrollView view) {
                if (mTableViewRangeListener != null) {
                    mTableViewRangeListener.onLeft(view);
                }
            }

            @Override
            public void onRight(HorizontalScrollView view) {
                if (mTableViewRangeListener != null) {
                    mTableViewRangeListener.onRight(view);
                }
            }
        });
        mTableViewAdapter.setOnTableViewCreatedListener(new TableViewAdapter.OnTableViewCreatedListener() {
            @Override
            public void onTableViewCreatedCompleted(CustomHorizontalScrollView mScrollView) {
                mScrollViews.add(mScrollView);
            }
        });
        mTableScrollView.setAdapter(mTableViewAdapter);

        mLockHeadView.setBackgroundColor(ContextCompat.getColor(mContext, mFristRowBackGroudColor));
        mUnLockHeadView.setBackgroundColor(ContextCompat.getColor(mContext, mFristRowBackGroudColor));
        if (isLockFristRow) {
            if (isLockFristColumn) {
                mLockHeadView.setVisibility(View.VISIBLE);
                mUnLockHeadView.setVisibility(View.GONE);
            } else {
                mLockHeadView.setVisibility(View.GONE);
                mUnLockHeadView.setVisibility(View.VISIBLE);
            }
            creatHeadView();
        } else {
            mLockHeadView.setVisibility(View.GONE);
            mUnLockHeadView.setVisibility(View.GONE);
        }
    }

    /**
     * 创建头部视图
     */
    private void creatHeadView() {
        if (isLockFristColumn) {
            mColumnTitleView.setTextColor(ContextCompat.getColor(mContext, mTableHeadTextColor));
            mColumnTitleView.setTextSize(TypedValue.COMPLEX_UNIT_SP, mTextViewSize);
            mColumnTitleView.setText(mColumnTitle);
            LinearLayout.LayoutParams layoutParams = (LinearLayout.LayoutParams) mColumnTitleView.getLayoutParams();
            layoutParams.width = DisplayUtil.dip2px(mContext, mColumnMaxWidths.get(0));
            layoutParams.height = DisplayUtil.dip2px(mContext, mRowMaxHeights.get(0));
            layoutParams.setMargins(mCellPadding, mCellPadding, mCellPadding, mCellPadding);
            mColumnTitleView.setLayoutParams(layoutParams);
            //构造滚动视图
            createScollview(mLockScrollView, mTableFristData, true);
            mScrollViews.add(mLockScrollView);
            mLockScrollView.setOnScrollChangeListener(new CustomHorizontalScrollView.onScrollChangeListener() {
                @Override
                public void onScrollChanged(HorizontalScrollView scrollView, int x, int y) {
                    changeAllScrollView(x, y);
                }

                @Override
                public void onScrollFarLeft(HorizontalScrollView scrollView) {
                    if (mTableViewRangeListener != null) {
                        mTableViewRangeListener.onLeft(scrollView);
                    }
                }

                @Override
                public void onScrollFarRight(HorizontalScrollView scrollView) {
                    if (mTableViewRangeListener != null) {
                        mTableViewRangeListener.onRight(scrollView);
                    }
                }
            });
        } else {
            createScollview(mUnLockScrollView, mTableFristData, true);
            mScrollViews.add(mUnLockScrollView);
            mUnLockScrollView.setOnScrollChangeListener(new CustomHorizontalScrollView.onScrollChangeListener() {
                @Override
                public void onScrollChanged(HorizontalScrollView scrollView, int x, int y) {
                    changeAllScrollView(x, y);
                }

                @Override
                public void onScrollFarLeft(HorizontalScrollView scrollView) {
                    if (mTableViewRangeListener != null) {
                        mTableViewRangeListener.onLeft(scrollView);
                    }
                }

                @Override
                public void onScrollFarRight(HorizontalScrollView scrollView) {
                    if (mTableViewRangeListener != null) {
                        mTableViewRangeListener.onRight(scrollView);
                    }
                }
            });
        }
    }


    /**
     * 改变所有滚动视图位置
     *
     * @param x
     * @param y
     */
    private void changeAllScrollView(int x, int y) {
        if (mScrollViews.size() > 0) {
            if (mTableViewListener != null) {
                mTableViewListener.onTableViewScrollChange(x, y);
            }
            for (int i = 0; i < mScrollViews.size(); i++) {
                HorizontalScrollView scrollView = mScrollViews.get(i);
                scrollView.scrollTo(x, y);
            }
        }
    }

    /**
     * 根据最大最小值，计算TextView的宽度
     *
     * @param textView
     * @param text
     * @return
     */
    private int measureTextWidth(TextView textView, String text) {
        if (textView != null) {
            LinearLayout.LayoutParams layoutParams = (LinearLayout.LayoutParams) textView.getLayoutParams();
            int width = DisplayUtil.px2dip(mContext, layoutParams.leftMargin) +
                    DisplayUtil.px2dip(mContext, layoutParams.rightMargin) +
                    getTextViewWidth(textView, text);
            if (width <= minColumnWidth) {
                return minColumnWidth;
            } else if (width > minColumnWidth && width <= maxColumnWidth) {
                return width;
            } else {
                return maxColumnWidth;
            }
        }
        return 0;
    }

    /**
     * 计算TextView高度
     *
     * @param textView
     * @param text
     * @return
     */
    private int measureTextHeight(TextView textView, String text) {
        if (textView != null) {
            LinearLayout.LayoutParams layoutParams = (LinearLayout.LayoutParams) textView.getLayoutParams();
            int height = getTextViewHeight(textView, text);
            if (height < minRowHeight) {
                return minRowHeight;
            } else if (height > minRowHeight && height < maxRowHeight) {
                return height;
            } else {
                return maxRowHeight;
            }
        }
        return 0;
    }

    /**
     * 根据文字计算textview的高度
     *
     * @param textView
     * @param text
     * @return
     */
    private int getTextViewHeight(TextView textView, String text) {
        if (textView != null) {
            int width = measureTextWidth(textView, text);
            TextPaint textPaint = textView.getPaint();
            StaticLayout staticLayout = new StaticLayout(text, textPaint, DisplayUtil.dip2px(mContext, width), Layout.Alignment.ALIGN_NORMAL, 1, 0, false);
            int height = DisplayUtil.px2dip(mContext, staticLayout.getHeight());
            return height;
        }
        return 0;
    }

    /**
     * 说明 根据文字和指定宽度计算高度
     * 作者 郭翰林
     * 创建时间 2018/3/15 下午12:39
     *
     * @param textView
     * @param text
     * @param width
     * @return
     */
    private int getTextViewHeight(TextView textView, String text, int width) {
        if (textView != null) {
            TextPaint textPaint = textView.getPaint();
            StaticLayout staticLayout = new StaticLayout(text, textPaint, DisplayUtil.dip2px(mContext, width), Layout.Alignment.ALIGN_NORMAL, 1, 0, false);
            int height = DisplayUtil.px2dip(mContext, staticLayout.getHeight());
            return height;
        }
        return 0;
    }

    /**
     * 根据文字计算textview的高度
     *
     * @param view
     * @param text
     * @return
     */
    private int getTextViewWidth(TextView view, String text) {
        if (view != null) {
            TextPaint paint = view.getPaint();
            return DisplayUtil.px2dip(mContext, (int) paint.measureText(text));
        }
        return 0;
    }


    /**
     * 构造滚动视图
     *
     * @param scrollView
     * @param datas
     * @param isFristRow 是否是第一行
     */
    private void createScollview(HorizontalScrollView scrollView, List<String> datas, boolean isFristRow) {
        //设置LinearLayout
        LinearLayout linearLayout = new LinearLayout(mContext);
        LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.MATCH_PARENT);
        linearLayout.setLayoutParams(layoutParams);
        linearLayout.setGravity(Gravity.CENTER);
        linearLayout.setOrientation(LinearLayout.HORIZONTAL);
        for (int i = 0; i < datas.size(); i++) {
            //构造单元格
            TextView textView = new TextView(mContext);
            if (isFristRow) {
                textView.setTextColor(ContextCompat.getColor(mContext, mTableHeadTextColor));
            } else {
                textView.setTextColor(ContextCompat.getColor(mContext, mTableContentTextColor));
            }
            textView.setTextSize(TypedValue.COMPLEX_UNIT_SP, mTextViewSize);
            textView.setGravity(Gravity.CENTER);
            textView.setText(datas.get(i));
            //设置布局
            LinearLayout.LayoutParams textViewParams = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT);
            textViewParams.setMargins(mCellPadding, mCellPadding, mCellPadding, mCellPadding);
            textView.setLayoutParams(textViewParams);
            ViewGroup.LayoutParams textViewParamsCopy = textView.getLayoutParams();
            if (isLockFristColumn) {
                textViewParamsCopy.width = DisplayUtil.dip2px(mContext, mColumnMaxWidths.get(i + 1));
            } else {
                textViewParamsCopy.width = DisplayUtil.dip2px(mContext, mColumnMaxWidths.get(i));
            }
            linearLayout.addView(textView);
            //画分隔线
            if (i != datas.size() - 1) {
                View splitView = new View(mContext);
                ViewGroup.LayoutParams splitViewParmas = new ViewGroup.LayoutParams(DisplayUtil.dip2px(mContext, 1),
                        ViewGroup.LayoutParams.MATCH_PARENT);
                splitView.setLayoutParams(splitViewParmas);
                if (isFristRow) {
                    splitView.setBackgroundColor(ContextCompat.getColor(mContext, R.color.white));
                } else {
                    splitView.setBackgroundColor(ContextCompat.getColor(mContext, R.color.light_gray));
                }
                linearLayout.addView(splitView);
            }
        }
        scrollView.addView(linearLayout);
    }

    /**
     * 说明 改变指定列指定宽度
     * 作者 郭翰林
     * 创建时间 2018/3/15 上午11:06
     *
     * @param mColumnNum
     * @param mColumnWidth
     */
    private void changeColumnWidth(int mColumnNum, int mColumnWidth) {
        if (mColumnMaxWidths != null && mColumnMaxWidths.size() > 0) {
            if (mColumnNum < mColumnMaxWidths.size() && mColumnNum >= 0) {
                mColumnMaxWidths.set(mColumnNum, mColumnWidth + DisplayUtil.px2dip(mContext, 15) * 2);
            } else {
                Log.e("LockTableView", "指定列数不存在");
            }
        }
    }


    //属性设置
    public LockTableView setLockFristRow(boolean lockFristRow) {
        isLockFristRow = lockFristRow;
        return this;
    }

    public LockTableView setLockFristColumn(boolean lockFristColumn) {
        isLockFristColumn = lockFristColumn;
        return this;
    }

    public LockTableView setMaxColumnWidth(int maxColumnWidth) {
        this.maxColumnWidth = maxColumnWidth;
        return this;
    }

    public LockTableView setMinColumnWidth(int minColumnWidth) {
        this.minColumnWidth = minColumnWidth;
        return this;
    }

    public LockTableView setFristRowBackGroudColor(int mFristRowBackGroudColor) {
        this.mFristRowBackGroudColor = mFristRowBackGroudColor;
        return this;
    }

    public LockTableView setNullableString(String mNullableString) {
        this.mNullableString = mNullableString;
        return this;
    }

    public LockTableView setTextViewSize(int mTextViewSize) {
        this.mTextViewSize = mTextViewSize;
        return this;
    }

    public LockTableView setTableHeadTextColor(int mTableHeadTextColor) {
        this.mTableHeadTextColor = mTableHeadTextColor;
        return this;
    }

    public LockTableView setTableContentTextColor(int mTableContentTextColor) {
        this.mTableContentTextColor = mTableContentTextColor;
        return this;
    }

    public LockTableView setMaxRowHeight(int maxRowHeight) {
        this.maxRowHeight = maxRowHeight;
        return this;
    }

    public LockTableView setMinRowHeight(int minRowHeight) {
        this.minRowHeight = minRowHeight;
        return this;
    }

    public LockTableView setTableViewListener(OnTableViewListener mTableViewListener) {
        this.mTableViewListener = mTableViewListener;
        return this;
    }

    public LockTableView setOnLoadingListener(OnLoadingListener mOnLoadingListener) {
        this.mOnLoadingListener = mOnLoadingListener;
        return this;
    }

    public LockTableView setTableViewRangeListener(OnTableViewRangeListener mTableViewRangeListener) {
        this.mTableViewRangeListener = mTableViewRangeListener;
        return this;
    }

    public LockTableView setOnItemClickListenter(OnItemClickListenter mOnItemClickListenter) {
        this.mOnItemClickListenter = mOnItemClickListenter;
        return this;
    }

    public LockTableView setOnItemLongClickListenter(OnItemLongClickListenter mOnItemLongClickListenter) {
        this.mOnItemLongClickListenter = mOnItemLongClickListenter;
        return this;
    }

    public LockTableView setOnItemSeletor(int mOnItemSeletor) {
        this.mOnItemSeletor = mOnItemSeletor;
        return this;
    }

    public LockTableView setCellPadding(int mCellPadding) {
        this.mCellPadding = DisplayUtil.dip2px(mContext,mCellPadding);
        return this;
    }

    /**
     * 指定第几列对应的宽度
     *
     * @param mColumnNum
     * @param mColumnWidth
     * @return
     */
    public LockTableView setColumnWidth(int mColumnNum, int mColumnWidth) {
        //判断是否已经设置过
        if (mChangeColumns.containsKey(mColumnNum)) {
            mChangeColumns.remove(mColumnNum);
        }
        mChangeColumns.put(mColumnNum, mColumnWidth);
        return this;
    }

    //值获取
    public ArrayList<Integer> getColumnMaxWidths() {
        return mColumnMaxWidths;
    }

    public ArrayList<Integer> getRowMaxHeights() {
        return mRowMaxHeights;
    }

    public LinearLayout getLockHeadView() {
        return mLockHeadView;
    }

    public LinearLayout getUnLockHeadView() {
        return mUnLockHeadView;
    }

    public XRecyclerView getTableScrollView() {
        return mTableScrollView;
    }

    public ArrayList<HorizontalScrollView> getScrollViews() {
        return mScrollViews;
    }

    /**
     * 说明 数据刷新时，重新设值
     * 作者 郭翰林
     * 创建时间 2017/9/17 下午2:33
     *
     * @param mTableDatas
     */
    public void setTableDatas(ArrayList<ArrayList<String>> mTableDatas) {
        this.mTableDatas = mTableDatas;
        mTableFristData.clear();
        mTableColumnDatas.clear();
        mTableRowDatas.clear();
        mColumnMaxWidths.clear();
        mRowMaxHeights.clear();
        initData();
        mTableViewAdapter.notifyDataSetChanged();
    }


    /**
     * 横向滚动监听
     */
    public interface OnTableViewListener {
        /**
         * 滚动监听
         *
         * @param x
         * @param y
         */
        void onTableViewScrollChange(int x, int y);
    }


    /**
     * 横向滚动视图滑动到边界的监听
     */
    public interface OnTableViewRangeListener {

        /**
         * 说明 最左侧
         * 作者 郭翰林
         * 创建时间 2017/12/14 下午4:45
         *
         * @param view
         */
        void onLeft(HorizontalScrollView view);

        /**
         * 说明 最右侧
         * 作者 郭翰林
         * 创建时间 2017/12/14 下午4:45
         *
         * @param view
         */
        void onRight(HorizontalScrollView view);

    }

    /**
     * 上拉刷新，下拉加载
     */
    public interface OnLoadingListener {

        /**
         * 说明 下拉刷新
         * 作者 郭翰林
         * 创建时间 2017/9/17 下午1:54
         *
         * @param mXRecyclerView
         * @param mTableDatas
         */
        void onRefresh(XRecyclerView mXRecyclerView, ArrayList<ArrayList<String>> mTableDatas);

        /**
         * 说明 上拉加载
         * 作者 郭翰林
         * 创建时间 2017/9/17 下午1:55
         *
         * @param mXRecyclerView
         * @param mTableDatas
         */
        void onLoadMore(XRecyclerView mXRecyclerView, ArrayList<ArrayList<String>> mTableDatas);
    }


    /**
     * 说明 Item点击事件
     * 作者 郭翰林
     * 创建时间 2018/2/2 下午4:50
     */
    public interface OnItemClickListenter {

        /**
         * @param item     点击项
         * @param position 点击位置
         */
        void onItemClick(View item, int position);
    }

    /**
     * 说明 Item长按事件
     * 作者 郭翰林
     * 创建时间 2018/2/2 下午4:50
     */
    public interface OnItemLongClickListenter {

        /**
         * @param item     点击项
         * @param position 点击位置
         */
        void onItemLongClick(View item, int position);
    }
}
