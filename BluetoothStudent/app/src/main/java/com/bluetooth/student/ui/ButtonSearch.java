
package com.bluetooth.student.ui;

import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;

import androidx.annotation.Nullable;
import androidx.appcompat.widget.AppCompatImageButton;

import java.util.ArrayList;

import com.bluetooth.student.R;

public class ButtonSearch extends AppCompatImageButton {
    private boolean searching = true;
    private boolean visible = true;
    private boolean animating = false;
    private ArrayList<CustomAnimator.EndListener> listeners = new ArrayList<>();
    private OnClickListener clickListener;
    private int drawableId = R.drawable.ic_cancel;


    public ButtonSearch(Context context) {
        super(context);
    }

    public ButtonSearch(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public ButtonSearch(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }


    public void setSearching(boolean searching, @Nullable CustomAnimator animator) {
        this.searching = searching;
        if (!animating && visible) {
            if (animator != null) {
                if (this.searching) {
                    if (drawableId != R.drawable.ic_cancel) {
                        animating = true;
                        animator.animateIconChange(this, getDrawable(R.drawable.ic_cancel), new CustomAnimator.EndListener() {
                            @Override
                            public void onAnimationEnd() {
                                animating = false;
                                drawableId = R.drawable.ic_cancel;
                                setVisible(visible, null);
                            }
                        });
                    } else {
                        setVisible(visible, null);
                    }
                } else {
                    if (drawableId != R.drawable.ic_search) {
                        animating = true;
                        animator.animateIconChange(this, getDrawable(R.drawable.ic_search), new CustomAnimator.EndListener() {
                            @Override
                            public void onAnimationEnd() {
                                animating = false;
                                drawableId = R.drawable.ic_search;
                                setVisible(visible, null);
                            }
                        });
                    } else {
                        setVisible(visible, null);
                    }
                }
            } else {
                if (this.isSearching()) {
                    setImageDrawable(getDrawable(R.drawable.ic_cancel));
                    drawableId = R.drawable.ic_cancel;
                } else {
                    setImageDrawable(getDrawable(R.drawable.ic_search));
                    drawableId = R.drawable.ic_search;
                }
            }
        }
    }

    public void setVisible(boolean visible, @Nullable final CustomAnimator.EndListener listener) {
        if (listener != null) {
            listeners.add(listener);
        }
        this.visible = visible;
        if (!animating) {
            if (isVisible()) {
                if (getVisibility() != VISIBLE) {
                    this.animating = true;
                    final Animation enlargeAnimation = AnimationUtils.loadAnimation(getContext(), R.anim.enlarge_icon);
                    enlargeAnimation.setDuration(getResources().getInteger(R.integer.durationShort));
                    startAnimation(enlargeAnimation);
                    getAnimation().setAnimationListener(new Animation.AnimationListener() {
                        @Override
                        public void onAnimationStart(Animation animation) {
                            setVisibility(VISIBLE);
                        }

                        @Override
                        public void onAnimationEnd(Animation animation) {
                            animating = false;
                            if (!isVisible()) {
                                setVisible(isVisible(), null);
                            } else {
                                notifySetVisibleSuccess();
                                setSearching(searching, null);
                            }
                        }

                        @Override
                        public void onAnimationRepeat(Animation animation) {
                        }
                    });
                } else {
                    notifySetVisibleSuccess();
                }
            } else {
                if (getVisibility() != GONE) {
                    this.animating = true;
                    final Animation dwindleAnimation = AnimationUtils.loadAnimation(getContext(), R.anim.dwindle_icon);
                    dwindleAnimation.setDuration(getResources().getInteger(R.integer.durationShort));
                    startAnimation(dwindleAnimation);
                    getAnimation().setAnimationListener(new Animation.AnimationListener() {
                        @Override
                        public void onAnimationStart(Animation animation) {
                        }

                        @Override
                        public void onAnimationEnd(Animation animation) {
                            setVisibility(GONE);
                            animating = false;
                            if (isVisible()) {
                                setVisible(isVisible(), null);
                            } else {
                                notifySetVisibleSuccess();
                            }
                        }

                        @Override
                        public void onAnimationRepeat(Animation animation) {
                        }
                    });
                } else {
                    notifySetVisibleSuccess();
                }
            }
        }
    }

    private void notifySetVisibleSuccess() {
        while (listeners.size() > 0) {
            listeners.remove(0).onAnimationEnd();
        }
    }

    @Override
    public void setOnClickListener(@Nullable OnClickListener l) {
        clickListener = l;
        super.setOnClickListener(clickListener);
    }

    public boolean isSearching() {
        return searching;
    }

    public boolean isVisible() {
        return visible;
    }

    public Drawable getDrawable(int id) {
        Drawable drawable = getResources().getDrawable(id, null);
        drawable.setTintList(getColorStateList(getContext(), R.color.primary));
        return drawable;
    }

    public static ColorStateList getColorStateList(Context context, int colorCode) {
        return context.getResources().getColorStateList(colorCode, null);
    }
}
