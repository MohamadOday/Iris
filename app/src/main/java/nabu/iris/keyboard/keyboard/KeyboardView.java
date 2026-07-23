/*
 * Copyright (C) 2010 The Android Open Source Project
 * Copyright (C) 2025 Raimondas Rimkus
 * Copyright (C) 2021 wittmane
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

package nabu.iris.keyboard.keyboard;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.TypedArray;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Paint.Align;
import android.graphics.PorterDuff;
import android.graphics.Rect;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.view.View;

import java.util.HashSet;

import nabu.iris.keyboard.R;
import nabu.iris.keyboard.compat.PreferenceManagerCompat;
import nabu.iris.keyboard.keyboard.internal.KeyDrawParams;
import nabu.iris.keyboard.keyboard.internal.KeyVisualAttributes;
import nabu.iris.keyboard.latin.common.Constants;
import nabu.iris.keyboard.latin.settings.Settings;
import nabu.iris.keyboard.latin.utils.TypefaceUtils;

/**
 * A view that renders a virtual {@link Keyboard}.
 *
 * @attr ref R.styleable#KeyboardView_keyBackground
 * @attr ref R.styleable#KeyboardView_functionalKeyBackground
 * @attr ref R.styleable#KeyboardView_spacebarBackground
 * @attr ref R.styleable#KeyboardView_spacebarIconWidthRatio
 * @attr ref R.styleable#Keyboard_Key_keyLabelFlags
 * @attr ref R.styleable#KeyboardView_keyHintLetterPadding
 * @attr ref R.styleable#KeyboardView_keyShiftedLetterHintPadding
 * @attr ref R.styleable#KeyboardView_keyTextShadowRadius
 * @attr ref R.styleable#KeyboardView_verticalCorrection
 * @attr ref R.styleable#Keyboard_Key_keyTypeface
 * @attr ref R.styleable#Keyboard_Key_keyLetterSize
 * @attr ref R.styleable#Keyboard_Key_keyLabelSize
 * @attr ref R.styleable#Keyboard_Key_keyLargeLetterRatio
 * @attr ref R.styleable#Keyboard_Key_keyLargeLabelRatio
 * @attr ref R.styleable#Keyboard_Key_keyHintLetterRatio
 * @attr ref R.styleable#Keyboard_Key_keyShiftedLetterHintRatio
 * @attr ref R.styleable#Keyboard_Key_keyHintLabelRatio
 * @attr ref R.styleable#Keyboard_Key_keyLabelOffCenterRatio
 * @attr ref R.styleable#Keyboard_Key_keyHintLabelOffCenterRatio
 * @attr ref R.styleable#Keyboard_Key_keyPreviewTextRatio
 * @attr ref R.styleable#Keyboard_Key_keyTextColor
 * @attr ref R.styleable#Keyboard_Key_keyTextColorDisabled
 * @attr ref R.styleable#Keyboard_Key_keyTextShadowColor
 * @attr ref R.styleable#Keyboard_Key_keyHintLetterColor
 * @attr ref R.styleable#Keyboard_Key_keyHintLabelColor
 * @attr ref R.styleable#Keyboard_Key_keyShiftedLetterHintInactivatedColor
 * @attr ref R.styleable#Keyboard_Key_keyShiftedLetterHintActivatedColor
 * @attr ref R.styleable#Keyboard_Key_keyPreviewTextColor
 */
public class KeyboardView extends View {
    // XML attributes
    private final KeyVisualAttributes mKeyVisualAttributes;
    // Default keyLabelFlags from {@link KeyboardTheme}.
    // Currently only "alignHintLabelToBottom" is supported.
    private final int mDefaultKeyLabelFlags;
    private final float mKeyHintLetterPadding;
    private final float mKeyShiftedLetterHintPadding;
    private final float mKeyTextShadowRadius;
    private final float mVerticalCorrection;
    private final Drawable mKeyBackground;
    private final Drawable mFunctionalKeyBackground;
    private final Drawable mSpacebarBackground;
    private final Rect mKeyBackgroundPadding = new Rect();
    private static final float KET_TEXT_SHADOW_RADIUS_DISABLED = -1.0f;
    protected int mCustomColor = 0;
    protected KeyboardTheme mTheme;

    // Personalization customizations
    private String mKeyShapeSetting = "default";
    private boolean mKeyBgImageActive = false;
    private android.graphics.Bitmap mCustomKeyBgBitmap = null;
    private int mKeyNormalColor = 0;
    private int mKeyPressedColor = 0;
    private boolean mColorsResolved = false;
    private String mKeySizeMode = "uniform";
    private int mKeySizeScale = 100;
    private int mKeyWidthScale = 100;
    private int mKeyHeightScale = 100;
    private int mKeyGapX = 2; // dp
    private int mKeyGapY = 3; // dp
    private boolean mKbdBgImageActive = false;
    private String mKbdBgImageStyle = "stretch";
    private android.graphics.Bitmap mCustomKbdBgBitmap = null;

    // The maximum key label width in the proportion to the key width.
    private static final float MAX_LABEL_RATIO = 0.90f;

    // Main keyboard
    // TODO: Consider having a dummy keyboard object to make this @NonNull
    private Keyboard mKeyboard;
    private final KeyDrawParams mKeyDrawParams = new KeyDrawParams();

    // Drawing
    /** True if all keys should be drawn */
    private boolean mInvalidateAllKeys;
    /** The keys that should be drawn */
    private final HashSet<Key> mInvalidatedKeys = new HashSet<>();
    /** The working rectangle for clipping */
    private final Rect mClipRect = new Rect();
    /** The keyboard bitmap buffer for faster updates */
    private Bitmap mOffscreenBuffer;
    /** The canvas for the above mutable keyboard bitmap */
    private final Canvas mOffscreenCanvas = new Canvas();
    private final Paint mPaint = new Paint();
    private final Paint.FontMetrics mFontMetrics = new Paint.FontMetrics();
    private final Paint mKeyFillPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint mKeyStrokePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint mKeyOverlayPaint = new Paint();

    public KeyboardView(final Context context, final AttributeSet attrs) {
        this(context, attrs, R.attr.keyboardViewStyle);
    }

    public KeyboardView(final Context context, final AttributeSet attrs, final int defStyle) {
        super(context, attrs, defStyle);

        final TypedArray keyboardViewAttr = context.obtainStyledAttributes(attrs,
                R.styleable.KeyboardView, defStyle, R.style.KeyboardView);
        mKeyBackground = keyboardViewAttr.getDrawable(R.styleable.KeyboardView_keyBackground);
        mKeyBackground.getPadding(mKeyBackgroundPadding);
        final Drawable functionalKeyBackground = keyboardViewAttr.getDrawable(
                R.styleable.KeyboardView_functionalKeyBackground);
        mFunctionalKeyBackground = (functionalKeyBackground != null) ? functionalKeyBackground
                : mKeyBackground;
        final Drawable spacebarBackground = keyboardViewAttr.getDrawable(
                R.styleable.KeyboardView_spacebarBackground);
        mSpacebarBackground = (spacebarBackground != null) ? spacebarBackground : mKeyBackground;
        mKeyHintLetterPadding = keyboardViewAttr.getDimension(
                R.styleable.KeyboardView_keyHintLetterPadding, 0.0f);
        mKeyShiftedLetterHintPadding = keyboardViewAttr.getDimension(
                R.styleable.KeyboardView_keyShiftedLetterHintPadding, 0.0f);
        mKeyTextShadowRadius = keyboardViewAttr.getFloat(
                R.styleable.KeyboardView_keyTextShadowRadius, KET_TEXT_SHADOW_RADIUS_DISABLED);
        mVerticalCorrection = keyboardViewAttr.getDimension(
                R.styleable.KeyboardView_verticalCorrection, 0.0f);
        keyboardViewAttr.recycle();

        final TypedArray keyAttr = context.obtainStyledAttributes(attrs,
                R.styleable.Keyboard_Key, defStyle, R.style.KeyboardView);
        mDefaultKeyLabelFlags = keyAttr.getInt(R.styleable.Keyboard_Key_keyLabelFlags, 0);
        mKeyVisualAttributes = KeyVisualAttributes.newInstance(keyAttr);
        keyAttr.recycle();

        mPaint.setAntiAlias(true);
    }

    private static void blendAlpha(final Paint paint, final int alpha) {
        final int color = paint.getColor();
        paint.setARGB((paint.getAlpha() * alpha) / Constants.Color.ALPHA_OPAQUE,
                Color.red(color), Color.green(color), Color.blue(color));
    }

    /**
     * Attaches a keyboard to this view. The keyboard can be switched at any time and the
     * view will re-layout itself to accommodate the keyboard.
     * @see Keyboard
     * @see #getKeyboard()
     * @param keyboard the keyboard to display in this view
     */
    public void setKeyboard(final Keyboard keyboard) {
        mKeyboard = keyboard;
        final int keyHeight = keyboard.mMostCommonKeyHeight;
        mKeyDrawParams.updateParams(keyHeight, mKeyVisualAttributes);
        mKeyDrawParams.updateParams(keyHeight, keyboard.mKeyVisualAttributes);
        final SharedPreferences prefs = PreferenceManagerCompat.getDeviceSharedPreferences(getContext());
        mCustomColor = Settings.readKeyboardColor(prefs, getContext());
        mTheme = Settings.getKeyboardTheme(getContext());

        mKeyShapeSetting = prefs.getString("pref_key_shape", "default");
        mKeyBgImageActive = prefs.getBoolean("pref_key_bg_image_active", false);
        mKbdBgImageActive = prefs.getBoolean("pref_kbd_bg_image_active", false);
        mKbdBgImageStyle = prefs.getString("pref_kbd_bg_image_style", "stretch");
        mKeySizeMode = prefs.getString("pref_key_size_mode", "uniform");
        mKeySizeScale = prefs.getInt("pref_key_size_scale", 100);
        mKeyWidthScale = prefs.getInt("pref_key_width_scale", 100);
        mKeyHeightScale = prefs.getInt("pref_key_height_scale", 100);
        mKeyGapX = prefs.getInt("pref_key_gap_x", 2);
        mKeyGapY = prefs.getInt("pref_key_gap_y", 3);
        loadPersonalizationAssets();
        mColorsResolved = false;

        invalidateAllKeys();
        requestLayout();
    }

    /**
     * Returns the current keyboard being displayed by this view.
     * @return the currently attached keyboard
     * @see #setKeyboard(Keyboard)
     */
    public Keyboard getKeyboard() {
        return mKeyboard;
    }

    protected float getVerticalCorrection() {
        return mVerticalCorrection;
    }

    protected KeyDrawParams getKeyDrawParams() {
        return mKeyDrawParams;
    }

    @Override
    protected void onMeasure(final int widthMeasureSpec, final int heightMeasureSpec) {
        final Keyboard keyboard = getKeyboard();
        if (keyboard == null) {
            super.onMeasure(widthMeasureSpec, heightMeasureSpec);
            return;
        }
        // The main keyboard expands to the entire this {@link KeyboardView}.
        final int width = keyboard.mOccupiedWidth + getPaddingLeft() + getPaddingRight();
        final int height = keyboard.mOccupiedHeight + getPaddingTop() + getPaddingBottom();
        setMeasuredDimension(width, height);
    }

    @Override
    protected void onDraw(final Canvas canvas) {
        super.onDraw(canvas);
        if (canvas.isHardwareAccelerated()) {
            onDrawKeyboard(canvas);
            return;
        }

        final boolean bufferNeedsUpdates = mInvalidateAllKeys || !mInvalidatedKeys.isEmpty();
        if (bufferNeedsUpdates || mOffscreenBuffer == null) {
            if (maybeAllocateOffscreenBuffer()) {
                mInvalidateAllKeys = true;
                // TODO: Stop using the offscreen canvas even when in software rendering
                mOffscreenCanvas.setBitmap(mOffscreenBuffer);
            }
            onDrawKeyboard(mOffscreenCanvas);
        }
        canvas.drawBitmap(mOffscreenBuffer, 0.0f, 0.0f, null);
    }

    private boolean maybeAllocateOffscreenBuffer() {
        final int width = getWidth();
        final int height = getHeight();
        if (width == 0 || height == 0) {
            return false;
        }
        if (mOffscreenBuffer != null && mOffscreenBuffer.getWidth() == width
                && mOffscreenBuffer.getHeight() == height) {
            return false;
        }
        freeOffscreenBuffer();
        mOffscreenBuffer = Bitmap.createBitmap(width, height, Bitmap.Config.RGB_565);
        return true;
    }

    private void freeOffscreenBuffer() {
        mOffscreenCanvas.setBitmap(null);
        mOffscreenCanvas.setMatrix(null);
        if (mOffscreenBuffer != null) {
            mOffscreenBuffer.recycle();
            mOffscreenBuffer = null;
        }
    }

    private void onDrawKeyboard(final Canvas canvas) {
        final Keyboard keyboard = getKeyboard();
        if (keyboard == null) {
            return;
        }

        // Dynamically load personalization preferences on draw to ensure live updates.
        final SharedPreferences prefs = PreferenceManagerCompat.getDeviceSharedPreferences(getContext());
        String currentShape = prefs.getString("pref_key_shape", "default");
        boolean currentBgActive = prefs.getBoolean("pref_key_bg_image_active", false);
        boolean currentKbdBgActive = prefs.getBoolean("pref_kbd_bg_image_active", false);
        String currentKbdBgStyle = prefs.getString("pref_kbd_bg_image_style", "stretch");
        String currentSizeMode = prefs.getString("pref_key_size_mode", "uniform");
        int currentSizeScale = prefs.getInt("pref_key_size_scale", 100);
        int currentWidthScale = prefs.getInt("pref_key_width_scale", 100);
        int currentHeightScale = prefs.getInt("pref_key_height_scale", 100);
        int currentGapX = prefs.getInt("pref_key_gap_x", 2);
        int currentGapY = prefs.getInt("pref_key_gap_y", 3);
        if (!currentShape.equals(mKeyShapeSetting) 
                || currentBgActive != mKeyBgImageActive 
                || currentKbdBgActive != mKbdBgImageActive 
                || !currentKbdBgStyle.equals(mKbdBgImageStyle)
                || !currentSizeMode.equals(mKeySizeMode)
                || currentSizeScale != mKeySizeScale
                || currentWidthScale != mKeyWidthScale
                || currentHeightScale != mKeyHeightScale
                || currentGapX != mKeyGapX
                || currentGapY != mKeyGapY
                || (mCustomKeyBgBitmap == null && currentBgActive)
                || (mCustomKbdBgBitmap == null && currentKbdBgActive)) {
            mKeyShapeSetting = currentShape;
            mKeyBgImageActive = currentBgActive;
            mKbdBgImageActive = currentKbdBgActive;
            mKbdBgImageStyle = currentKbdBgStyle;
            mKeySizeMode = currentSizeMode;
            mKeySizeScale = currentSizeScale;
            mKeyWidthScale = currentWidthScale;
            mKeyHeightScale = currentHeightScale;
            mKeyGapX = currentGapX;
            mKeyGapY = currentGapY;
            loadPersonalizationAssets();
            mColorsResolved = false;
        }

        final Paint paint = mPaint;
        final Drawable background = getBackground();
        if (background != null && mTheme.mCustomColorSupport) {
            if (keyboard.getClass() == MoreKeysKeyboard.class) {
                background.setColorFilter(mCustomColor, PorterDuff.Mode.OVERLAY);
            } else {
                setBackgroundColor(mCustomColor);
            }
        }
        // Calculate clip region and set.
        final boolean drawAllKeys = mInvalidateAllKeys || mInvalidatedKeys.isEmpty();
        final boolean isHardwareAccelerated = canvas.isHardwareAccelerated();
        // TODO: Confirm if it's really required to draw all keys when hardware acceleration is on.
        if (drawAllKeys || isHardwareAccelerated) {
            if (!isHardwareAccelerated && background != null) {
                // Need to draw keyboard background on {@link #mOffscreenBuffer}.
                canvas.drawColor(Color.BLACK, PorterDuff.Mode.CLEAR);
                background.draw(canvas);
            }
            drawKbdBg(canvas);
            // Draw all keys.
            for (final Key key : keyboard.getSortedKeys()) {
                onDrawKey(key, canvas, paint);
            }
        } else {
            for (final Key key : mInvalidatedKeys) {
                if (!keyboard.hasKey(key)) {
                    continue;
                }
                if (background != null) {
                    // Need to redraw key's background on {@link #mOffscreenBuffer}.
                    final int x = key.getX() + getPaddingLeft();
                    final int y = key.getY() + getPaddingTop();
                    mClipRect.set(x, y, x + key.getWidth(), y + key.getHeight());
                    canvas.save();
                    canvas.clipRect(mClipRect);
                    canvas.drawColor(Color.BLACK, PorterDuff.Mode.CLEAR);
                    background.draw(canvas);
                    drawKbdBg(canvas);
                    canvas.restore();
                }
                onDrawKey(key, canvas, paint);
            }
        }

        mInvalidatedKeys.clear();
        mInvalidateAllKeys = false;
    }

    private void onDrawKey(final Key key, final Canvas canvas,
            final Paint paint) {
        final int keyDrawX = key.getX() + getPaddingLeft();
        final int keyDrawY = key.getY() + getPaddingTop();
        canvas.translate(keyDrawX, keyDrawY);

        final KeyVisualAttributes attr = key.getVisualAttributes();
        final KeyDrawParams params = mKeyDrawParams.mayCloneAndUpdateParams(key.getHeight(), attr);
        params.mAnimAlpha = Constants.Color.ALPHA_OPAQUE;

        if (!key.isSpacer()) {
            final Drawable background = key.selectBackgroundDrawable(
                    mKeyBackground, mFunctionalKeyBackground, mSpacebarBackground);
            if (background != null) {
                onDrawKeyBackground(key, canvas, background);
            }
        }
        onDrawKeyTopVisuals(key, canvas, paint, params);

        canvas.translate(-keyDrawX, -keyDrawY);
    }

    // Draw key background.
    protected void onDrawKeyBackground(final Key key, final Canvas canvas,
            final Drawable background) {
        final int keyWidth = key.getWidth();
        final int keyHeight = key.getHeight();
        final Rect padding = mKeyBackgroundPadding;
        final int bgWidth = keyWidth + padding.left + padding.right;
        final int bgHeight = keyHeight + padding.top + padding.bottom;
        final int bgX = -padding.left;
        final int bgY = -padding.top;

        boolean isCustomScale = "custom".equals(mKeySizeMode);
        boolean hasScale = (isCustomScale && (mKeyWidthScale != 100 || mKeyHeightScale != 100)) 
                || (!isCustomScale && mKeySizeScale != 100);

        if ("default".equals(mKeyShapeSetting) && !mKeyBgImageActive && !hasScale && mKeyGapX == 2 && mKeyGapY == 3) {
            final Rect bounds = background.getBounds();
            if (bgWidth != bounds.right || bgHeight != bounds.bottom) {
                background.setBounds(0, 0, bgWidth, bgHeight);
            }
            canvas.translate(bgX, bgY);
            background.draw(canvas);
            canvas.translate(-bgX, -bgY);
            return;
        }

        if (!mColorsResolved) {
            final android.util.TypedValue typedValue = new android.util.TypedValue();
            if (getContext().getTheme().resolveAttribute(R.attr.keyNormalBackgroundColor, typedValue, true)) {
                mKeyNormalColor = typedValue.data;
            } else {
                mKeyNormalColor = 0x33FFFFFF;
            }
            if (getContext().getTheme().resolveAttribute(R.attr.keyPressedBackgroundColor, typedValue, true)) {
                mKeyPressedColor = typedValue.data;
            } else {
                mKeyPressedColor = 0x66FFFFFF;
            }
            mColorsResolved = true;
        }

        float density = getResources().getDisplayMetrics().density;
        
        // Draw custom shape inside custom padding boundaries
        float gapX = mKeyGapX * density;
        float gapY = mKeyGapY * density;
        float left = bgX + gapX;
        float top = bgY + gapY;
        float right = bgX + bgWidth - gapX;
        float bottom = bgY + bgHeight - gapY;

        if (left >= right || top >= bottom) {
            left = 0;
            top = 0;
            right = keyWidth;
            bottom = keyHeight;
        }

        // Apply Key Size Scale preference around center of bounds
        if (hasScale) {
            float scaleX, scaleY;
            if (isCustomScale) {
                scaleX = mKeyWidthScale / 100.0f;
                scaleY = mKeyHeightScale / 100.0f;
            } else {
                scaleX = mKeySizeScale / 100.0f;
                scaleY = mKeySizeScale / 100.0f;
            }
            float cx = left + (right - left) / 2.0f;
            float cy = top + (bottom - top) / 2.0f;
            float halfW = ((right - left) / 2.0f) * scaleX;
            float halfH = ((bottom - top) / 2.0f) * scaleY;
            left = cx - halfW;
            right = cx + halfW;
            top = cy - halfH;
            bottom = cy + halfH;
        }

        android.graphics.Path path = new android.graphics.Path();
        if ("circle".equals(mKeyShapeSetting)) {
            float cx = left + (right - left) / 2.0f;
            float cy = top + (bottom - top) / 2.0f;
            float radius = Math.min(right - left, bottom - top) / 2.0f;
            path.addCircle(cx, cy, radius, android.graphics.Path.Direction.CW);
        } else if ("squircle".equals(mKeyShapeSetting)) {
            path = getSquirclePath(left, top, right, bottom);
        } else if ("hexagon".equals(mKeyShapeSetting)) {
            path = getHexagonPath(left, top, right, bottom);
        } else if ("square".equals(mKeyShapeSetting)) {
            path.addRect(left, top, right, bottom, android.graphics.Path.Direction.CW);
        } else { // "default" or "rounded"
            float radius = getResources().getDimension(R.dimen.button_corner_radius_lxx);
            if (hasScale) {
                float avgScale = isCustomScale ? (mKeyWidthScale + mKeyHeightScale) / 200.0f : mKeySizeScale / 100.0f;
                radius = radius * avgScale;
            }
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP) {
                path.addRoundRect(left, top, right, bottom, radius, radius, android.graphics.Path.Direction.CW);
            } else {
                path.addRoundRect(new android.graphics.RectF(left, top, right, bottom), radius, radius, android.graphics.Path.Direction.CW);
            }
        }

        mKeyFillPaint.setStyle(Paint.Style.FILL);
        if (key.isPressed()) {
            mKeyFillPaint.setColor(mKeyPressedColor);
        } else {
            mKeyFillPaint.setColor(mKeyNormalColor);
        }
        canvas.drawPath(path, mKeyFillPaint);

        if (mKeyBgImageActive && mCustomKeyBgBitmap != null) {
            canvas.save();
            canvas.clipPath(path);
            canvas.drawBitmap(mCustomKeyBgBitmap, null, new android.graphics.RectF(left, top, right, bottom), null);
            if (key.isPressed()) {
                mKeyOverlayPaint.setColor(0x40000000);
                canvas.drawRect(left, top, right, bottom, mKeyOverlayPaint);
            }
            canvas.restore();
        }

        mKeyStrokePaint.setStyle(Paint.Style.STROKE);
        mKeyStrokePaint.setStrokeWidth(1.0f * density);
        boolean isDarkTheme = isColorDark(mKeyNormalColor);
        mKeyStrokePaint.setColor(isDarkTheme ? 0x1AFFFFFF : 0x12000000);
        canvas.drawPath(path, mKeyStrokePaint);
    }

    // Draw key top visuals.
    protected void onDrawKeyTopVisuals(final Key key,final Canvas canvas,
            final Paint paint, final KeyDrawParams params) {
        final int keyWidth = key.getWidth();
        final int keyHeight = key.getHeight();
        final float centerX = keyWidth * 0.5f;
        final float centerY = keyHeight * 0.5f;

        // Draw key label.
        final Keyboard keyboard = getKeyboard();
        final Drawable icon = (keyboard == null) ? null
                : key.getIcon(keyboard.mIconsSet, params.mAnimAlpha);
        float labelX = centerX;
        float labelBaseline = centerY;
        final String label = key.getLabel();
        if (label != null) {
            paint.setTypeface(key.selectTypeface(params));
            paint.setTextSize(key.selectTextSize(params));
            final float labelCharHeight = TypefaceUtils.getReferenceCharHeight(paint);
            final float labelCharWidth = TypefaceUtils.getReferenceCharWidth(paint);

            // Vertical label text alignment.
            labelBaseline = centerY + labelCharHeight / 2.0f;

            // Horizontal label text alignment
            if (key.isAlignLabelOffCenter()) {
                // The label is placed off center of the key. Used mainly on "phone number" layout.
                labelX = centerX + params.mLabelOffCenterRatio * labelCharWidth;
                paint.setTextAlign(Align.LEFT);
            } else {
                labelX = centerX;
                paint.setTextAlign(Align.CENTER);
            }
            if (key.needsAutoXScale()) {
                final float ratio = Math.min(1.0f, (keyWidth * MAX_LABEL_RATIO) /
                        TypefaceUtils.getStringWidth(label, paint));
                if (key.needsAutoScale()) {
                    final float autoSize = paint.getTextSize() * ratio;
                    paint.setTextSize(autoSize);
                } else {
                    paint.setTextScaleX(ratio);
                }
            }

            paint.setColor(key.selectTextColor(params));
            // Set a drop shadow for the text if the shadow radius is positive value.
            if (mKeyTextShadowRadius > 0.0f) {
                paint.setShadowLayer(mKeyTextShadowRadius, 0.0f, 0.0f, params.mTextShadowColor);
            } else {
                paint.clearShadowLayer();
            }

            blendAlpha(paint, params.mAnimAlpha);
            canvas.drawText(label, 0, label.length(), labelX, labelBaseline, paint);
            // Turn off drop shadow and reset x-scale.
            paint.clearShadowLayer();
            paint.setTextScaleX(1.0f);
        }

        // Draw hint label.
        final String hintLabel = key.getHintLabel();
        if (hintLabel != null) {
            paint.setTextSize(key.selectHintTextSize(params));
            paint.setColor(key.selectHintTextColor(params));
            // TODO: Should add a way to specify type face for hint letters
            paint.setTypeface(Typeface.DEFAULT_BOLD);
            blendAlpha(paint, params.mAnimAlpha);
            final float labelCharHeight = TypefaceUtils.getReferenceCharHeight(paint);
            final float labelCharWidth = TypefaceUtils.getReferenceCharWidth(paint);
            final float hintX, hintBaseline;
            if (key.hasHintLabel()) {
                // The hint label is placed just right of the key label. Used mainly on
                // "phone number" layout.
                hintX = labelX + params.mHintLabelOffCenterRatio * labelCharWidth;
                if (key.isAlignHintLabelToBottom(mDefaultKeyLabelFlags)) {
                    hintBaseline = labelBaseline;
                } else {
                    hintBaseline = centerY + labelCharHeight / 2.0f;
                }
                paint.setTextAlign(Align.LEFT);
            } else if (key.hasShiftedLetterHint()) {
                // The hint label is placed at top-right corner of the key. Used mainly on tablet.
                hintX = keyWidth - mKeyShiftedLetterHintPadding - labelCharWidth / 2.0f;
                paint.getFontMetrics(mFontMetrics);
                hintBaseline = -mFontMetrics.top;
                paint.setTextAlign(Align.CENTER);
            } else { // key.hasHintLetter()
                // The hint letter is placed at top-right corner of the key. Used mainly on phone.
                final float hintDigitWidth = TypefaceUtils.getReferenceDigitWidth(paint);
                final float hintLabelWidth = TypefaceUtils.getStringWidth(hintLabel, paint);
                hintX = keyWidth - mKeyHintLetterPadding
                        - Math.max(hintDigitWidth, hintLabelWidth) / 2.0f;
                hintBaseline = -paint.ascent();
                paint.setTextAlign(Align.CENTER);
            }
            final float adjustmentY = params.mHintLabelVerticalAdjustment * labelCharHeight;
            canvas.drawText(
                    hintLabel, 0, hintLabel.length(), hintX, hintBaseline + adjustmentY, paint);
        }

        // Draw key icon.
        if (label == null && icon != null) {
            final int iconWidth = Math.min(icon.getIntrinsicWidth(), keyWidth);
            final int iconHeight = icon.getIntrinsicHeight();
            final int iconY;
            if (key.isAlignIconToBottom()) {
                iconY = keyHeight - iconHeight;
            } else {
                iconY = (keyHeight - iconHeight) / 2; // Align vertically center.
            }
            final int iconX = (keyWidth - iconWidth) / 2; // Align horizontally center.
            drawIcon(canvas, icon, iconX, iconY, iconWidth, iconHeight);
        }
    }

    protected static void drawIcon(final Canvas canvas, final Drawable icon,
            final int x, final int y, final int width, final int height) {
        canvas.translate(x, y);
        icon.setBounds(0, 0, width, height);
        icon.draw(canvas);
        canvas.translate(-x, -y);
    }

    public Paint newLabelPaint(final Key key) {
        final Paint paint = new Paint();
        paint.setAntiAlias(true);
        if (key == null) {
            paint.setTypeface(mKeyDrawParams.mTypeface);
            paint.setTextSize(mKeyDrawParams.mLabelSize);
        } else {
            paint.setColor(key.selectTextColor(mKeyDrawParams));
            paint.setTypeface(key.selectTypeface(mKeyDrawParams));
            paint.setTextSize(key.selectTextSize(mKeyDrawParams));
        }
        return paint;
    }

    /**
     * Requests a redraw of the entire keyboard. Calling {@link #invalidate} is not sufficient
     * because the keyboard renders the keys to an off-screen buffer and an invalidate() only
     * draws the cached buffer.
     * @see #invalidateKey(Key)
     */
    public void invalidateAllKeys() {
        mInvalidatedKeys.clear();
        mInvalidateAllKeys = true;
        invalidate();
    }

    /**
     * Invalidates a key so that it will be redrawn on the next repaint. Use this method if only
     * one key is changing it's content. Any changes that affect the position or size of the key
     * may not be honored.
     * @param key key in the attached {@link Keyboard}.
     * @see #invalidateAllKeys
     */
    public void invalidateKey(final Key key) {
        if (mInvalidateAllKeys || key == null) {
            return;
        }
        mInvalidatedKeys.add(key);
        final int x = key.getX() + getPaddingLeft();
        final int y = key.getY() + getPaddingTop();
        invalidate(x, y, x + key.getWidth(), y + key.getHeight());
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        freeOffscreenBuffer();
        if (mCustomKeyBgBitmap != null) {
            mCustomKeyBgBitmap.recycle();
            mCustomKeyBgBitmap = null;
        }
        if (mCustomKbdBgBitmap != null) {
            mCustomKbdBgBitmap.recycle();
            mCustomKbdBgBitmap = null;
        }
    }

    public void deallocateMemory() {
        freeOffscreenBuffer();
        if (mCustomKeyBgBitmap != null) {
            mCustomKeyBgBitmap.recycle();
            mCustomKeyBgBitmap = null;
        }
        if (mCustomKbdBgBitmap != null) {
            mCustomKbdBgBitmap.recycle();
            mCustomKbdBgBitmap = null;
        }
    }

    private void loadPersonalizationAssets() {
        if (mCustomKeyBgBitmap != null) {
            mCustomKeyBgBitmap.recycle();
            mCustomKeyBgBitmap = null;
        }
        if (mKeyBgImageActive) {
            try {
                java.io.File file = new java.io.File(getContext().getFilesDir(), "key_background_custom.png");
                if (file.exists()) {
                    mCustomKeyBgBitmap = android.graphics.BitmapFactory.decodeFile(file.getAbsolutePath());
                }
            } catch (Exception e) {
                android.util.Log.e("KeyboardView", "Failed to load custom key background bitmap", e);
            }
        }

        if (mCustomKbdBgBitmap != null) {
            mCustomKbdBgBitmap.recycle();
            mCustomKbdBgBitmap = null;
        }
        if (mKbdBgImageActive) {
            try {
                java.io.File file = new java.io.File(getContext().getFilesDir(), "keyboard_background_custom.png");
                if (file.exists()) {
                    mCustomKbdBgBitmap = android.graphics.BitmapFactory.decodeFile(file.getAbsolutePath());
                }
            } catch (Exception e) {
                android.util.Log.e("KeyboardView", "Failed to load custom keyboard background bitmap", e);
            }
        }
    }

    private android.graphics.Path getSquirclePath(float left, float top, float right, float bottom) {
        android.graphics.Path path = new android.graphics.Path();
        float width = right - left;
        float height = bottom - top;
        float cx = left + width / 2.0f;
        float cy = top + height / 2.0f;
        float rx = width / 2.0f;
        float ry = height / 2.0f;

        int steps = 64;
        double n = 4.0;
        for (int i = 0; i < steps; i++) {
            double angle = (2.0 * Math.PI * i) / steps;
            double cos = Math.cos(angle);
            double sin = Math.sin(angle);

            double x = Math.signum(cos) * Math.pow(Math.abs(cos), 2.0 / n) * rx;
            double y = Math.signum(sin) * Math.pow(Math.abs(sin), 2.0 / n) * ry;

            if (i == 0) {
                path.moveTo((float)(cx + x), (float)(cy + y));
            } else {
                path.lineTo((float)(cx + x), (float)(cy + y));
            }
        }
        path.close();
        return path;
    }

    private android.graphics.Path getHexagonPath(float left, float top, float right, float bottom) {
        android.graphics.Path path = new android.graphics.Path();
        float width = right - left;
        float height = bottom - top;
        float cy = top + height / 2.0f;

        path.moveTo(left + width / 4.0f, top);
        path.lineTo(right - width / 4.0f, top);
        path.lineTo(right, cy);
        path.lineTo(right - width / 4.0f, bottom);
        path.lineTo(left + width / 4.0f, bottom);
        path.lineTo(left, cy);
        path.close();
        return path;
    }

    private static boolean isColorDark(final int color) {
        final double darkness = 1 - (0.299 * Color.red(color) + 0.587 * Color.green(color) + 0.114 * Color.blue(color)) / 255.0;
        return darkness >= 0.5;
    }

    private android.graphics.Rect getCenterCropRect(int bitmapWidth, int bitmapHeight, int viewWidth, int viewHeight) {
        float bitmapRatio = (float) bitmapWidth / bitmapHeight;
        float viewRatio = (float) viewWidth / viewHeight;
        int srcLeft = 0, srcTop = 0, srcRight = bitmapWidth, srcBottom = bitmapHeight;
        if (bitmapRatio > viewRatio) {
            int newWidth = (int) (bitmapHeight * viewRatio);
            srcLeft = (bitmapWidth - newWidth) / 2;
            srcRight = srcLeft + newWidth;
        } else {
            int newHeight = (int) (bitmapWidth / viewRatio);
            srcTop = (bitmapHeight - newHeight) / 2;
            srcBottom = srcTop + newHeight;
        }
        return new android.graphics.Rect(srcLeft, srcTop, srcRight, srcBottom);
    }

    private void drawKbdBg(final Canvas canvas) {
        if (mKbdBgImageActive && mCustomKbdBgBitmap != null) {
            if ("crop".equals(mKbdBgImageStyle)) {
                android.graphics.Rect srcRect = getCenterCropRect(mCustomKbdBgBitmap.getWidth(), mCustomKbdBgBitmap.getHeight(), getWidth(), getHeight());
                canvas.drawBitmap(mCustomKbdBgBitmap, srcRect, new android.graphics.Rect(0, 0, getWidth(), getHeight()), null);
            } else {
                canvas.drawBitmap(mCustomKbdBgBitmap, null, new android.graphics.Rect(0, 0, getWidth(), getHeight()), null);
            }
        }
    }
}
