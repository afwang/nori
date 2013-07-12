package pe.moe.nori.widgets;

import android.content.Context;
import android.util.AttributeSet;
import com.android.volley.toolbox.NetworkImageView;

public class SquaredNetworkImageView extends NetworkImageView {

  public SquaredNetworkImageView(Context context) {
    super(context);
  }

  public SquaredNetworkImageView(Context context, AttributeSet attrs) {
    super(context, attrs);
  }

  public SquaredNetworkImageView(Context context, AttributeSet attrs, int defStyle) {
    super(context, attrs, defStyle);
  }

  @Override
  public void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
    super.onMeasure(widthMeasureSpec, heightMeasureSpec);
    setMeasuredDimension(getMeasuredWidth(), getMeasuredWidth());
  }
}