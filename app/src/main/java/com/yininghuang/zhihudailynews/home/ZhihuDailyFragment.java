package com.yininghuang.zhihudailynews.home;

import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.design.widget.Snackbar;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.yininghuang.zhihudailynews.BaseActivity;
import com.yininghuang.zhihudailynews.BaseFragment;
import com.yininghuang.zhihudailynews.R;
import com.yininghuang.zhihudailynews.adapter.ZhihuLatestAdapter;
import com.yininghuang.zhihudailynews.detail.ZhihuNewsDetailActivity;
import com.yininghuang.zhihudailynews.model.ZhihuLatestNews;
import com.yininghuang.zhihudailynews.utils.ItemDecoration;
import com.yininghuang.zhihudailynews.widget.AutoLoadRecyclerView;

import java.lang.reflect.Type;
import java.util.List;

/**
 * Created by Yining Huang on 2016/10/17.
 */

public class ZhihuDailyFragment extends BaseFragment implements ZhihuDailyContract.View, ZhihuLatestAdapter.OnItemClickListener {

    private AutoLoadRecyclerView mContentRec;
    private SwipeRefreshLayout mSwipeRefreshLayout;

    private ZhihuDailyContract.Presenter mPresenter;
    private ZhihuLatestAdapter mAdapter;

    private int mCurrentDy = 0;

    public static ZhihuDailyFragment newInstance() {
        return new ZhihuDailyFragment();
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_zhihudaily, container, false);
        initViews(rootView);
        if (savedInstanceState != null) {
            mPresenter.queryReadId();
            Type type = new TypeToken<List<ZhihuLatestNews>>() {
            }.getType();
            List<ZhihuLatestNews> data = new Gson().fromJson(savedInstanceState.getString("data"), type);
            mAdapter.addNewsList(data);
            mAdapter.notifyDataSetChanged();
            mCurrentDy = savedInstanceState.getInt("dy");
            mContentRec.scrollTo(0, mCurrentDy);
        } else {
            mPresenter.init();
        }

        return rootView;
    }

    private void initViews(View rootView) {
        mContentRec = (AutoLoadRecyclerView) rootView.findViewById(R.id.contentRec);
        mSwipeRefreshLayout = (SwipeRefreshLayout) rootView.findViewById(R.id.swipeLayout);
        mContentRec.setLayoutManager(new LinearLayoutManager(getActivity()));
        mAdapter = new ZhihuLatestAdapter(getActivity());
        mAdapter.setOnItemClickListener(this);
        mContentRec.setAdapter(mAdapter);
        RecyclerView.ItemDecoration itemDecoration;
        if (((BaseActivity) getActivity()).getThemeId() == BaseActivity.DARK_THEME)
            itemDecoration = new ItemDecoration(getActivity(), R.color.colorDividerDark);
        else itemDecoration = new ItemDecoration(getActivity(), R.color.colorDivider);
        mContentRec.addItemDecoration(itemDecoration);

        mContentRec.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrolled(RecyclerView recyclerView, int dx, int dy) {
                super.onScrolled(recyclerView, dx, dy);
                mCurrentDy = +dy;
            }
        });

        mContentRec.setOnLoadingListener(() -> {
            if (!mContentRec.isRefreshing())
                mPresenter.queryHistoryStory(mAdapter.getOldestNewsDate());
        });
        mSwipeRefreshLayout.setOnRefreshListener(() -> mPresenter.reload());
    }

    @Override
    public void showStories(ZhihuLatestNews stories) {
        mAdapter.getLatestNewsList().clear();
        mAdapter.getZhihuStoryList().clear();
        mAdapter.addNews(stories);
        mAdapter.notifyDataSetChanged();
    }

    @Override
    public void addHistoryStories(ZhihuLatestNews stories) {
        mAdapter.addNews(stories);
        mAdapter.notifyItemRangeInserted(mAdapter.getZhihuStoryList().size() - stories.getStories().size() + 1, stories.getStories().size());
    }

    @Override
    public void setReadIdList(List<String> list) {
        List<String> data = mAdapter.getReadIdList();
        data.clear();
        data.addAll(list);
    }

    @Override
    public void setHistoryLoadingStatus(boolean status) {
        mContentRec.setRefresh(status);
    }

    @Override
    public void setLoadingStatus(boolean status) {
        mSwipeRefreshLayout.setRefreshing(status);
    }

    @Override
    public void setLoadingComplete() {
        mContentRec.setLoadComplete();
        mAdapter.setLoadComplete();
    }

    @Override
    public void showLoadError() {
        if (getView() != null)
            Snackbar.make(getView(), R.string.load_error, Snackbar.LENGTH_LONG)
                    .setAction(R.string.refresh, view -> mPresenter.reload()).show();
    }

    @Override
    public void setPresenter(ZhihuDailyContract.Presenter presenter) {
        mPresenter = presenter;
    }

    @Override
    public void onResume() {
        super.onResume();
        mAdapter.notifyDataSetChanged();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        mPresenter.onStop();
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        if (mAdapter.getLatestNewsList().size() != 0) {
            outState.putString("data", new Gson().toJson(mAdapter.getLatestNewsList()));
            outState.putInt("dy", mCurrentDy);
        }
    }

    @Override
    public void onPosterClick(ZhihuLatestNews.ZhihuTopStory topStory) {
        Intent intent = new Intent(getActivity(), ZhihuNewsDetailActivity.class);
        intent.putExtra("id", topStory.getId());
        startActivity(intent);
    }

    @Override
    public void onNewsClick(ZhihuLatestNews.ZhihuStory story) {
        mAdapter.getReadIdList().add(String.valueOf(story.getId()));
        Intent intent = new Intent(getActivity(), ZhihuNewsDetailActivity.class);
        intent.putExtra("id", story.getId());
        startActivity(intent);
    }
}
