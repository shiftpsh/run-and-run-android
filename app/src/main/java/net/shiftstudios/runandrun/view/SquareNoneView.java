package net.shiftstudios.runandrun.view;

import android.content.Context;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;

public final class SquareNoneView extends View {

	private static final String TAG = SquareNoneView.class.getSimpleName();

	public SquareNoneView(Context context) {
		super(context);
	}

	public SquareNoneView(Context context, AttributeSet attrs) {
		super(context, attrs);
	}

	public SquareNoneView(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);
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
	protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
		Log.d(TAG, "Width spec: " + MeasureSpec.toString(widthMeasureSpec));
		Log.d(TAG, "Height spec: " + MeasureSpec.toString(heightMeasureSpec));
		
		int widthMode = MeasureSpec.getMode(widthMeasureSpec);
		int widthSize = MeasureSpec.getSize(widthMeasureSpec);
		
		int heightMode = MeasureSpec.getMode(heightMeasureSpec);
		int heightSize = MeasureSpec.getSize(heightMeasureSpec);
		
		int chosenWidth = chooseDimension(widthMode, widthSize);
		int chosenHeight = chooseDimension(heightMode, heightSize);
		
		int chosenDimension = Math.max(chosenWidth, chosenHeight);
		
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

}
