package fm.a2d.sf.view;

import android.content.Context;
import android.graphics.*;
import android.util.AttributeSet;
import android.util.Log;
import android.widget.TextView;

public class GradientTextView extends TextView {

  private int mFrom = Color.WHITE;
  private int mTo = Color.BLACK;

  private LinearGradient mGradient = null;

  public GradientTextView(Context context) {
    super(context, null, -1);
  }

  public GradientTextView(Context context, AttributeSet attrs) {
    super(context, attrs, -1);
  }

  public GradientTextView(Context context, AttributeSet attrs, int defStyle) {
    super(context, attrs, defStyle);
    mGradient = new LinearGradient(0, 0, 0, getHeight(), mFrom, mTo, Shader.TileMode.CLAMP);
  }

  public GradientTextView setColors(int from, int to) {
    mFrom = from;
    mTo = to;
    mGradient = new LinearGradient(0, 0, 0, getHeight(), mFrom, mTo, Shader.TileMode.CLAMP);
    return this;
  }

  @Override
  protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
    super.onLayout(changed, left, top, right, bottom);
    if (changed && mGradient != null) {
      getPaint().setShader(mGradient);
    }
  }

  @Override
  protected void onDraw(Canvas canvas) {
    canvas.save();

    float py = getHeight() / 2.0f;
    float px = getWidth() / 2.0f;

    Matrix matrix = new Matrix();
    matrix.preScale(-1, 1);
    canvas.setMatrix(matrix);

    super.onDraw(canvas);

    canvas.restore();
  }
}