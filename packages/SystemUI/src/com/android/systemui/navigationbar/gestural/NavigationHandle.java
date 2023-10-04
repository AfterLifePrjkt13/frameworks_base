/*
 * Copyright (C) 2020 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.android.systemui.navigationbar.gestural;

import android.animation.ArgbEvaluator;
import android.annotation.ColorInt;
import android.content.Context;
import android.content.res.Resources;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.view.ContextThemeWrapper;
import android.view.View;

import com.android.settingslib.Utils;
import com.android.systemui.R;
import com.android.systemui.navigationbar.buttons.ButtonInterface;

import com.android.systemui.Dependency;
import com.android.systemui.tuner.TunerService;

import com.declan.prjct.utils.DeclanUtils;

public class NavigationHandle extends View implements ButtonInterface, TunerService.Tunable {

    private final Context mContext;
    protected final Paint mPaint = new Paint();
    private @ColorInt final int mLightColor;
    private @ColorInt final int mDarkColor;
    private boolean mRequiresInvalidate;

    public int mRadius, mBottom, mGestureCustomBottom, mGestureCustomRadius, mGestureCustomWidth;
	public boolean mGestureCustomPillHandle;

    public NavigationHandle(Context context) {
        this(context, null);
    }

    public NavigationHandle(Context context, AttributeSet attr) {
        super(context, attr);
        mContext = context;

        final int dualToneDarkTheme = Utils.getThemeAttr(context, R.attr.darkIconTheme);
        final int dualToneLightTheme = Utils.getThemeAttr(context, R.attr.lightIconTheme);
        Context lightContext = new ContextThemeWrapper(context, dualToneLightTheme);
        Context darkContext = new ContextThemeWrapper(context, dualToneDarkTheme);
        mLightColor = Utils.getColorAttrDefaultColor(lightContext, R.attr.homeHandleColor);
        mDarkColor = Utils.getColorAttrDefaultColor(darkContext, R.attr.homeHandleColor);
        mPaint.setAntiAlias(true);
        setFocusable(false);
    }

    private void updateGestureHandle() {
		invalidate();
        final Resources res = mContext.getResources();
		if (mGestureCustomPillHandle) {
            mRadius = DeclanUtils.getValueInDp(mGestureCustomRadius);
            mBottom = DeclanUtils.getValueInDp(mGestureCustomBottom);
            getLayoutParams().width = DeclanUtils.getValueInDp(mGestureCustomWidth);
		} else {
            mRadius = res.getDimensionPixelSize(R.dimen.navigation_handle_radius);
            mBottom = res.getDimensionPixelSize(R.dimen.navigation_handle_bottom);
            getLayoutParams().width = res.getDimensionPixelSize(R.dimen.navigation_home_handle_width);
            }
		requestLayout();
	}

	@Override
	public void onTuningChanged(String key, String newValue) {
		if ("system:declan_gesture_navbar_switch".equals(key)) {
			mGestureCustomPillHandle = TunerService.parseIntegerSwitch(newValue, false);
			updateGestureHandle();
		} else if ("system:declan_gesture_navbar_radius".equals(key)) {
			mGestureCustomRadius = TunerService.parseInteger(newValue, 1);
			updateGestureHandle();
		} else if ("system:declan_gesture_navbar_bottom".equals(key)) {
			mGestureCustomBottom = TunerService.parseInteger(newValue, 6);
			updateGestureHandle();
		} else if ("system:declan_gesture_navbar_lenght".equals(key)) {
			mGestureCustomWidth = TunerService.parseInteger(newValue, 72);
			updateGestureHandle();
		}
	}

    @Override
    public void setAlpha(float alpha) {
        super.setAlpha(alpha);
        if (alpha > 0f && mRequiresInvalidate) {
            mRequiresInvalidate = false;
            invalidate();
        }
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        // Draw that bar
        int navHeight = getHeight();
        float height = mRadius * 2;
        int width = getWidth();
        float y = (navHeight - mBottom - height);
        canvas.drawRoundRect(0, y, width, y + height, mRadius, mRadius, mPaint);
    }

    @Override
    public void setImageDrawable(Drawable drawable) {
    }

    @Override
    public void abortCurrentGesture() {
    }

    @Override
    public void setVertical(boolean vertical) {
    }

    @Override
    public void setDarkIntensity(float intensity) {
        int color = (int) ArgbEvaluator.getInstance().evaluate(intensity, mLightColor, mDarkColor);
        if (mPaint.getColor() != color) {
            mPaint.setColor(color);
            if (getVisibility() == VISIBLE && getAlpha() > 0) {
                invalidate();
            } else {
                // If we are currently invisible, then invalidate when we are next made visible
                mRequiresInvalidate = true;
            }
        }
    }

    @Override
    public void setDelayTouchFeedback(boolean shouldDelay) {
    }

    @Override
    public void onAttachedToWindow() {
        super.onAttachedToWindow();
		Dependency.get(TunerService.class).addTunable(this, new String[]{"system:declan_gesture_navbar_switch", "system:declan_gesture_navbar_radius", "system:declan_gesture_navbar_bottom", "system:declan_gesture_navbar_lenght"});
    }

	@Override
    public void onDetachedFromWindow() {
        super.onDetachedFromWindow();
		((TunerService) Dependency.get(TunerService.class)).removeTunable(this);
    }
}
