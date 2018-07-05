package com.example.xyzreader.ui;

import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.Rect;
import android.graphics.Typeface;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.Fragment;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.Loader;
import android.support.v7.graphics.Palette;
import android.text.Html;
import android.text.format.DateUtils;
import android.transition.Slide;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AnimationUtils;
import android.widget.TextView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.animation.GlideAnimation;
import com.bumptech.glide.request.target.SimpleTarget;
import com.example.xyzreader.R;
import com.example.xyzreader.data.ArticleLoader;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.GregorianCalendar;

/**
 * A fragment representing a single Article detail screen. This fragment is
 * either contained in a {@link ArticleListActivity} in two-pane mode (on
 * tablets) or a {@link ArticleDetailActivity} on handsets.
 */
public class ArticleDetailFragment extends Fragment implements
        LoaderManager.LoaderCallbacks<Cursor> {
    private static final String TAG = "ArticleDetailFragment";

    public static final String ARG_ITEM_ID = "item_id";
    private static final float PARALLAX_FACTOR = 1.25f;

    private Cursor mCursor;
    private long mItemId;
    private View mRootView;
    private int mMutedColor = 0xFF333333;
    private DrawInsetsFrameLayout mDrawInsetsLayout;
    private ColorDrawable mStatusBarColorDrawable;

    private int mTopInset;
    private int mScrollY;
    private boolean mIsCard = false;
    private int mStatusBarFullOpacityBottom;

    private SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.sss");
    // Use default locale format
    private SimpleDateFormat outputFormat = new SimpleDateFormat();
    // Most time functions can only handle 1902 - 2037
    private GregorianCalendar START_OF_EPOCH = new GregorianCalendar(2,1,1);

    private CharSequence mTitle;
    private CharSequence mByLine;
    private String mPhotoUrl;
    private float mAspectRatio = 1.5f;

    /**
     * Mandatory empty constructor for the fragment manager to instantiate the
     * fragment (e.g. upon screen orientation changes).
     */
    public ArticleDetailFragment() {
    }

    public static ArticleDetailFragment newInstance(long itemId) {
        Bundle arguments = new Bundle();
        arguments.putLong(ARG_ITEM_ID, itemId);
        ArticleDetailFragment fragment = new ArticleDetailFragment();
        fragment.setArguments(arguments);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (getArguments().containsKey(ARG_ITEM_ID)) {
            mItemId = getArguments().getLong(ARG_ITEM_ID);
        }

        mIsCard = getResources().getBoolean(R.bool.detail_is_card);
        mStatusBarFullOpacityBottom = getResources().getDimensionPixelSize(
                R.dimen.detail_card_top_margin);
        setHasOptionsMenu(true);

        /*
         * Enter animations
         */
//        Slide slideUp = new Slide(Gravity.BOTTOM);
//        slideUp.addTarget(R.id.article_body);
//        slideUp.setInterpolator(AnimationUtils.loadInterpolator(getActivity(),
//                android.R.interpolator.linear_out_slow_in));
//        slideUp.setDuration(getResources().getInteger(R.integer.anim_duration));
//        getActivity().getWindow().setEnterTransition(slideUp);
    }

    public ArticleDetailActivity getActivityCast() {
        return (ArticleDetailActivity) getActivity();
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        // In support library r8, calling initLoader for a fragment in a FragmentPagerAdapter in
        // the fragment's onCreate may cause the same LoaderManager to be dealt to multiple
        // fragments because their mIndex is -1 (haven't been added to the activity yet). Thus,
        // we do this in onActivityCreated.
        getLoaderManager().initLoader(0, null, this);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
        mRootView = inflater.inflate(R.layout.fragment_article_detail, container, false);
        mDrawInsetsLayout = (DrawInsetsFrameLayout)
                mRootView.findViewById(R.id.draw_insets_frame_layout);
        mDrawInsetsLayout.setOnInsetsCallback(new DrawInsetsFrameLayout.OnInsetsCallback() {
            @Override
            public void onInsetsChanged(Rect insets) {
                mTopInset = insets.top;
            }
        });

        mStatusBarColorDrawable = new ColorDrawable(0);

        bindViews();
        updateStatusBar();
        return mRootView;
    }

    private void updateStatusBar() {
        int color = 0;
        if (mTopInset != 0 && mScrollY > 0) {
            float f = progress(mScrollY,
                    mStatusBarFullOpacityBottom - mTopInset * 3,
                    mStatusBarFullOpacityBottom - mTopInset);
            color = Color.argb((int) (255 * f),
                    (int) (Color.red(mMutedColor) * 0.9),
                    (int) (Color.green(mMutedColor) * 0.9),
                    (int) (Color.blue(mMutedColor) * 0.9));
        }
        mStatusBarColorDrawable.setColor(color);
        mDrawInsetsLayout.setInsetBackground(mStatusBarColorDrawable);
    }

    static float progress(float v, float min, float max) {
        return constrain((v - min) / (max - min), 0, 1);
    }

    static float constrain(float val, float min, float max) {
        if (val < min) {
            return min;
        } else if (val > max) {
            return max;
        } else {
            return val;
        }
    }

    private Date parsePublishedDate() {
        try {
            String date = mCursor.getString(ArticleLoader.Query.PUBLISHED_DATE);
            return dateFormat.parse(date);
        } catch (ParseException ex) {
            Log.e(TAG, ex.getMessage());
            Log.i(TAG, "passing today's date");
            return new Date();
        }
    }

    private void bindViews() {
        if (mRootView == null) {
            return;
        }

        TextView bodyView = (TextView) mRootView.findViewById(R.id.article_body);
        // JB: Overrides style's android:fontFamily
        // FIXME Disabled displaying article for debugging
//        bodyView.setTypeface(Typeface.createFromAsset(getResources().getAssets(), "Rosario-Regular.ttf"));

        if (mCursor != null) {
            mRootView.setAlpha(0);
            mRootView.setVisibility(View.VISIBLE);
            mRootView.animate().alpha(1);
            mTitle = mCursor.getString(ArticleLoader.Query.TITLE);
            Date publishedDate = parsePublishedDate();
            if (!publishedDate.before(START_OF_EPOCH.getTime())) {
                mByLine = Html.fromHtml(
                        DateUtils.getRelativeTimeSpanString(
                                publishedDate.getTime(),
                                System.currentTimeMillis(), DateUtils.HOUR_IN_MILLIS,
                                DateUtils.FORMAT_ABBREV_ALL).toString()
                                + " by <font color='#ffffff'>"
                                + mCursor.getString(ArticleLoader.Query.AUTHOR)
                                + "</font>");

            } else {
                // If date is before 1902, just show the string
                mByLine = Html.fromHtml(
                        outputFormat.format(publishedDate) + " by <font color='#ffffff'>"
                        + mCursor.getString(ArticleLoader.Query.AUTHOR)
                                + "</font>");

            }
            // FIXME Disabled displaying article for debugging
//            bodyView.setText(Html.fromHtml(mCursor.getString(ArticleLoader.Query.BODY).replaceAll("(\r\n|\n)", "<br />")));
            mPhotoUrl = mCursor.getString(ArticleLoader.Query.PHOTO_URL);
            mAspectRatio = mCursor.getFloat(ArticleLoader.Query.ASPECT_RATIO);
            updateActivity();

//            ImageLoaderHelper.getInstance(getActivity()).getImageLoader()
//                    .get(mCursor.getString(ArticleLoader.Query.PHOTO_URL), new ImageLoader.ImageListener() {
//                        @Override
//                        public void onResponse(final ImageLoader.ImageContainer imageContainer, boolean b) {
//                            Bitmap bitmap = imageContainer.getBitmap();
//                            if (bitmap != null) {
//                                new Palette.Builder(bitmap)
//                                        .maximumColorCount(12)
//                                        .generate(new Palette.PaletteAsyncListener() {
//                                            @Override
//                                            public void onGenerated(@NonNull Palette palette) {
//                                                mMutedColor = palette
//                                                        .getDarkMutedColor(0xFF333333);
//                                                mBitmap = imageContainer.getBitmap();
//                                                updateActivity();
//                                                updateStatusBar();
//                                            }
//                                        });
//                            }
//                        }
//
//                        @Override
//                        public void onErrorResponse(VolleyError volleyError) {
//
//                        }
//                    });
        } else {
            mRootView.setVisibility(View.GONE);
            mTitle = "";
            mByLine = "";
            bodyView.setText("");
            updateActivity();

        }
    }

    private boolean shouldUpdateActivity() {
        boolean activityExists = getActivityCast() != null;
        boolean userVisibleHint = getUserVisibleHint();
        return activityExists && userVisibleHint;
    }

    private void updateActivity() {
        if (shouldUpdateActivity()) {
            getActivityCast().setHeaderTitle(mTitle);
            getActivityCast().setToolbarTitle(mTitle);
            getActivityCast().setHeaderByLineText(mByLine);
            updateActivityImage(mPhotoUrl, mAspectRatio);
        }
    }

    private void updateActivityImage(String imageUrl, float aspectRatio) {

        SimpleTarget<Bitmap> target = new SimpleTarget<Bitmap>() {
            @Override
            public void onResourceReady(final Bitmap bitmap, GlideAnimation glideAnimation) {
                if (bitmap != null) {
                    new Palette.Builder(bitmap)
                            .maximumColorCount(12)
                            .generate(new Palette.PaletteAsyncListener() {
                                @Override
                                public void onGenerated(@NonNull Palette palette) {
                                    mMutedColor = palette
                                            .getDarkMutedColor(0xFF333333);
                                    getActivityCast().getPhotoView().setImageBitmap(bitmap);
                                    getActivityCast().setToolbarColor(mMutedColor);
                                    updateStatusBar();
                                }
                            });
                }
            }
        };

        DisplayMetrics displayMetrics = new DisplayMetrics();
        getActivity().getWindowManager().getDefaultDisplay().getMetrics(displayMetrics);

        int measuredWidth = displayMetrics.widthPixels;
        int measuredHeight  = (int) (measuredWidth / aspectRatio);

        Glide.with(this)
                .load(imageUrl)
                .asBitmap()
                // resizes the image to these dimensions (in pixel). does not respect aspect ratio
                .override(measuredWidth, measuredHeight)
                .into(target);
    }

    @Override
    public void setUserVisibleHint(boolean isVisibleToUser) {
        super.setUserVisibleHint(isVisibleToUser);
        if (isVisibleToUser) {
            updateActivity();
        }
    }

    @Override
    public Loader<Cursor> onCreateLoader(int i, Bundle bundle) {
        return ArticleLoader.newInstanceForItemId(getActivity(), mItemId);
    }

    @Override
    public void onLoadFinished(Loader<Cursor> cursorLoader, Cursor cursor) {
        if (!isAdded()) {
            if (cursor != null) {
                cursor.close();
            }
            return;
        }

        mCursor = cursor;
        if (mCursor != null && !mCursor.moveToFirst()) {
            Log.e(TAG, "Error reading item detail cursor");
            mCursor.close();
            mCursor = null;
        }

        bindViews();
    }

    @Override
    public void onLoaderReset(Loader<Cursor> cursorLoader) {
        mCursor = null;
        bindViews();
    }

    public int getUpButtonFloor() {
        return 300;
//        if (mPhotoContainerView == null || mPhotoView.getHeight() == 0) {
//            return Integer.MAX_VALUE;
//        }
//
//        // account for parallax
//        return mIsCard
//                ? (int) mPhotoContainerView.getTranslationY() + mPhotoView.getHeight() - mScrollY
//                : mPhotoView.getHeight() - mScrollY;
    }
}
