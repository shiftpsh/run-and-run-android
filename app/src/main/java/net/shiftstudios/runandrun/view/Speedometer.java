package net.shiftstudios.runandrun.view;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.graphics.Typeface;
import android.os.Bundle;
import android.os.Handler;
import android.os.Parcelable;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;

import java.text.DecimalFormat;

public final class Speedometer extends View {

	private static final String TAG = Speedometer.class.getSimpleName();
	
	private Handler handler;

	// drawing tools
	private RectF rimRect;
	private RectF faceRect;

	private Paint scalePaint;
    private Paint textPaint;
    private Paint speedPaintBig;
    private Paint speedPaintNormal;
    private Paint speedPaintSmall;
	private RectF scaleRect;
	
	private Paint handPaint;
	private Path handPath;
	private Paint handScrewPaint;
	
	// scale configuration
	private static final int totalNicks = 60;
	private static final float degreesPerNick = 360.0f / totalNicks;	
	private static final int centerDegree = 40; // the one in the top center (12 o'clock)
	private static final int minDegrees = 0;
	private static final int maxDegrees = 80;

    float sDelta = 0;
	
	// hand dynamics -- all are angular expressed in F degrees
	private boolean handInitialized = true;
	private float handPosition = centerDegree;
	private float handTarget = centerDegree;
	private long lastHandMoveTime = -1L;

    public int[] colors = {
            0xff455a64,
            0xff03a9f4,
            0xff00bcd4,
            0xff26a69a,
            0xff4caf50,
            0xff8bc34a,
            0xffcddc39,
            0xffffc107,
            0xffff8f00,
            0xffff5722,
            0xfff44336,
            0xffe91e63,
            0xff9c27b0,
            0xff8131b4,
            0xff673ab7,
            0xff5631ac,
            0xff4527a0,
            0xff3b2199,
            0xff311b92};

	public Speedometer(Context context) {
		super(context);
		init();
	}

	public Speedometer(Context context, AttributeSet attrs) {
		super(context, attrs);
		init();
	}

	public Speedometer(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);
		init();
	}
	
	@Override
	protected void onAttachedToWindow() {
		super.onAttachedToWindow();
	}

	@Override
	protected void onDetachedFromWindow() {
		super.onDetachedFromWindow();
	}
	
	@Override
	protected void onRestoreInstanceState(Parcelable state) {
		Bundle bundle = (Bundle) state;
		Parcelable superState = bundle.getParcelable("superState");
		super.onRestoreInstanceState(superState);
		
		handInitialized = bundle.getBoolean("handInitialized");
		handPosition = bundle.getFloat("handPosition");
		handTarget = bundle.getFloat("handTarget");
		lastHandMoveTime = bundle.getLong("lastHandMoveTime");
	}

	@Override
	protected Parcelable onSaveInstanceState() {
		Parcelable superState = super.onSaveInstanceState();
		
		Bundle state = new Bundle();
		state.putParcelable("superState", superState);
		state.putBoolean("handInitialized", handInitialized);
		state.putFloat("handPosition", handPosition);
		state.putFloat("handTarget", handTarget);
		state.putLong("lastHandMoveTime", lastHandMoveTime);
		return state;
	}

	private void init() {
		handler = new Handler();
		
		initDrawingTools();
	}

	private void initDrawingTools() {
        this.setLayerType(View.LAYER_TYPE_SOFTWARE, null);

		rimRect = new RectF(0.1f, 0.1f, 0.9f, 0.9f);

		// the linear gradient is a bit skewed for realism
		float rimSize = 0.02f;
		faceRect = new RectF();
		faceRect.set(rimRect.left + rimSize, rimRect.top + rimSize,
                rimRect.right - rimSize, rimRect.bottom - rimSize);

		scalePaint = new Paint();
		scalePaint.setStyle(Paint.Style.STROKE);
        scalePaint.setColor(getColor(handPosition));
		scalePaint.setStrokeWidth(0.005f);
		scalePaint.setAntiAlias(true);

        textPaint = new Paint();
        textPaint.setTextSize(0.045f);
        textPaint.setColor(0xFF222222);
        textPaint.setTextAlign(Paint.Align.CENTER);
        textPaint.setAntiAlias(true);

        speedPaintBig = new Paint();
        speedPaintBig.setTextSize(0.15f);
        speedPaintBig.setColor(0xFF222222);
        speedPaintBig.setTextAlign(Paint.Align.RIGHT);
        speedPaintBig.setAntiAlias(true);

        speedPaintNormal = new Paint();
        speedPaintNormal.setTextSize(0.075f);
        speedPaintNormal.setColor(0xFF222222);
        speedPaintNormal.setTextAlign(Paint.Align.LEFT);
        speedPaintNormal.setAntiAlias(true);

        speedPaintSmall = new Paint();
        speedPaintSmall.setTextSize(0.035f);
        speedPaintSmall.setColor(0xFF222222);
        speedPaintSmall.setTextAlign(Paint.Align.LEFT);
        speedPaintSmall.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.ITALIC));
        speedPaintSmall.setAntiAlias(true);

		float scalePosition = 0.10f;
		scaleRect = new RectF();
		scaleRect.set(faceRect.left + scalePosition, faceRect.top + scalePosition,
                faceRect.right - scalePosition, faceRect.bottom - scalePosition);

		handPaint = new Paint();
		handPaint.setAntiAlias(true);
		handPaint.setColor(getColor(handPosition));
		handPaint.setShadowLayer(0.01f, -0.005f, -0.005f, 0x7f000000);
		handPaint.setStyle(Paint.Style.FILL);	
		
		handPath = new Path();
		handPath.moveTo(0.5f, 0.5f + 0.2f);
		handPath.lineTo(0.5f - 0.010f, 0.5f + 0.2f - 0.007f);
		handPath.lineTo(0.5f - 0.002f, 0.5f - 0.32f);
		handPath.lineTo(0.5f + 0.002f, 0.5f - 0.32f);
		handPath.lineTo(0.5f + 0.010f, 0.5f + 0.2f - 0.007f);
		handPath.lineTo(0.5f, 0.5f + 0.2f);
		handPath.addCircle(0.5f, 0.5f, 0.025f, Path.Direction.CW);
		
		handScrewPaint = new Paint();
		handScrewPaint.setAntiAlias(true);
		handScrewPaint.setColor(0xffffffff);
		handScrewPaint.setStyle(Paint.Style.FILL);
	}
	
	@Override
	protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
		Log.d(TAG, "Width spec: " + MeasureSpec.toString(widthMeasureSpec));
		Log.d(TAG, "Height spec: " + MeasureSpec.toString(heightMeasureSpec));
		
		int widthMode = MeasureSpec.getMode(widthMeasureSpec);
		int widthSize = MeasureSpec.getSize(widthMeasureSpec);
		
		int heightMode = MeasureSpec.getMode(heightMeasureSpec);
		int heightSize = MeasureSpec.getSize(heightMeasureSpec);
		
		int chosenWidth = chooseDimension(widthMode, widthSize);
		int chosenHeight = chooseDimension(heightMode, heightSize);
		
		int chosenDimension = Math.min(chosenWidth, chosenHeight);
		
		setMeasuredDimension(chosenDimension, chosenDimension);
	}
	
	private int chooseDimension(int mode, int size) {
		if (mode == MeasureSpec.AT_MOST || mode == MeasureSpec.EXACTLY) {
			return size;
		} else { // (mode == MeasureSpec.UNSPECIFIED)
			return getPreferredSize();
		} 
	}
	
	// in case there is no size specified
	private int getPreferredSize() {
		return 300;
	}

    private void drawSpeed(Canvas canvas) {
        canvas.save(Canvas.MATRIX_SAVE_FLAG);

        double speed = handPosition;
        DecimalFormat df = new DecimalFormat("0.0");
        String[] formatted = df.format(speed).split("[.]");

        drawTextOnCanvasWithMagnifier(canvas, formatted[0], 0.545f, 0.7f, speedPaintBig);
        drawTextOnCanvasWithMagnifier(canvas, "." + formatted[1], 0.555f, 0.65f, speedPaintNormal);
        drawTextOnCanvasWithMagnifier(canvas, "km/h", 0.555f, 0.7f, speedPaintSmall);

        canvas.restore();
    }

	private void drawScale(Canvas canvas) {
		canvas.save(Canvas.MATRIX_SAVE_FLAG);

		for (int i = 0; i < totalNicks; ++i) {
			float y1 = scaleRect.top;
			float y2 = y1 - 0.03f;

            int value = nickToDegree(i);
            double opacity = (25d - Math.abs(value - handPosition)) / 25d;
            if (opacity < 0) opacity = 0;
            if (value >= minDegrees && value <= maxDegrees) {
                int color = getColor(handPosition);
                int R = Color.red(color);
                int G = Color.green(color);
                int B = Color.blue(color);

                scalePaint.setColor(Color.argb((int) (opacity * 255), R, G, B));
                canvas.drawLine(0.5f, y1, 0.5f, y2, scalePaint);
            }

			if (i % 5 == 0 && value >= minDegrees && value <= maxDegrees) {
                String valueString = value + "";
                textPaint.setColor(Color.argb((int) (opacity * 255), 34, 34, 34));
                drawTextOnCanvasWithMagnifier(canvas, valueString, 0.5f, y2 - 0.015f, textPaint);
            }
			
			canvas.rotate(degreesPerNick, 0.5f, 0.5f);
		}
		canvas.restore();		
	}
	
	private int nickToDegree(int nick) {
		int rawDegree = ((nick < totalNicks / 2) ? nick : (nick - totalNicks)) * 2;
		int shiftedDegree = rawDegree + centerDegree;
		return shiftedDegree;
	}
	
	private float degreeToAngle(float degree) {
		return (degree - centerDegree) / 2.0f * degreesPerNick;
	}
	
	private void drawHand(Canvas canvas) {
		if (handInitialized) {
            handPaint.setColor(getColor(handPosition));

			float handAngle = degreeToAngle(handPosition);
			canvas.save(Canvas.MATRIX_SAVE_FLAG);
			canvas.rotate(handAngle, 0.5f, 0.5f);
			canvas.drawPath(handPath, handPaint);
			canvas.restore();
			
			canvas.drawCircle(0.5f, 0.5f, 0.01f, handScrewPaint);
		}
	}

	@Override
	protected void onDraw(Canvas canvas) {
		float scale = (float) getWidth();
		canvas.save(Canvas.MATRIX_SAVE_FLAG);
		canvas.scale(scale, scale);

        drawScale(canvas);
        drawSpeed(canvas);
        drawHand(canvas);

		canvas.restore();
	
		if (handNeedsToMove()) {
			moveHand();
		}
	}

	@Override
	protected void onSizeChanged(int w, int h, int oldw, int oldh) {
		Log.d(TAG, "Size changed to " + w + "x" + h);
	}
	

	private boolean handNeedsToMove() {
		return Math.abs(handPosition - handTarget) > 0.01f;
	}
	
	private void moveHand() {
		if (!handNeedsToMove()) {
			return;
		}

		if (lastHandMoveTime != -1L) {
			long currentTime = System.currentTimeMillis();
			float delta = (currentTime - lastHandMoveTime) / 1000.0f;

			handPosition += (handTarget - handPosition) / 2.5f;
            sDelta += delta;
			if (Math.abs(handTarget - handPosition) < 0.01f | sDelta > 0.5f) {
				handPosition = handTarget;
				lastHandMoveTime = -1L;
                sDelta = 0;
			} else {
				lastHandMoveTime = System.currentTimeMillis();				
			}
			invalidate();
		} else {
			lastHandMoveTime = System.currentTimeMillis();
			moveHand();
		}
	}
	
	private float getRelativeTemperaturePosition() {
		if (handPosition < centerDegree) {
			return - (centerDegree - handPosition) / (float) (centerDegree - minDegrees);
		} else {
			return (handPosition - centerDegree) / (float) (maxDegrees - centerDegree);
		}
	}
	
	public void setHandTarget(float speed) {
        /*
		if (speed < minDegrees) {
			speed = minDegrees;
		} else if (speed > maxDegrees) {
			speed = maxDegrees;
		}
		*/
		handTarget = speed;
		handInitialized = true;
        invalidate();
	}

    public static void drawTextOnCanvasWithMagnifier(Canvas canvas, String text, float x, float y, Paint paint) {
        if (android.os.Build.VERSION.SDK_INT <= 15) {
            canvas.drawText(text, x, y, paint);
        } else {
            float originalTextSize = paint.getTextSize();
            final float magnifier = 100f;
            canvas.save();

            canvas.scale(1f / magnifier, 1f / magnifier);
            paint.setTextSize(originalTextSize * magnifier);
            canvas.drawText(text, x * magnifier, y * magnifier, paint);

            canvas.restore();
            paint.setTextSize(originalTextSize);
        }
    }

    public int getColor(double value){
        int colorMode = (int) (value / 5);
        if (colorMode + 1 > colors.length - 1) {
            return colors[colors.length - 1];
        } else {
            double mixValue = (value % 5) / 5;
            int R = (int) ((double) Color.red(colors[colorMode + 1]) * (mixValue) + (double) Color.red(colors[colorMode]) * (1 - mixValue));
            int G = (int) ((double) Color.green(colors[colorMode + 1]) * (mixValue) + (double) Color.green(colors[colorMode]) * (1 - mixValue));
            int B = (int) ((double) Color.blue(colors[colorMode + 1]) * (mixValue) + (double) Color.blue(colors[colorMode]) * (1 - mixValue));
            return Color.argb(255, R, G, B);
        }
    }

}
