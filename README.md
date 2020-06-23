# GooView
消息提示标


# 粘性控件效果(该效果重在理解思路，代码不要求实现)  #

### 1.首先自定义GooView类，继承View，并初始化画笔 ###

	private void init(){
	    paint = new Paint(Paint.ANTI_ALIAS_FLAG);
	    paint.setColor(Color.RED);
	}

### 2.在onDraw中先绘制出2个红色的圆，并指定圆心坐标和半径 ###

	//1.绘制2个圆
	//绘制拖拽圆
	canvas.drawCircle(dragCenter.x,dragCenter.y, dragRadius, paint);
	//绘制粘性圆
	canvas.drawCircle(gooCenter.x,gooCenter.y, gooRadius, paint);

### 3.重点来了，使用贝塞尔曲线绘制2个圆连接的部分，Path类提供了绘制贝塞尔曲线的方法，需要我们指定起点，控制点，终点，最后我们需要绘制2条贝塞尔曲线才能组成2圆连接的部分 ###


	//使用贝塞尔曲线绘制2个圆链接的部分
	Path path = new Path();
	path.moveTo(gooPoints[0].x, gooPoints[0].y);//移动到起点
	//绘制第1条曲线
	path.quadTo(controlPoint.x, controlPoint.y, dragPoints[0].x, dragPoints[0].y);//使用曲线经过指定控制点链接到终点;
	path.lineTo(dragPoints[1].x, dragPoints[1].y);//链接到指定点
	//绘制第2条曲线
	path.quadTo(controlPoint.x, controlPoint.y, gooPoints[1].x, gooPoints[1].y);
	//path.close();//闭合，默认自动闭合
	canvas.drawPath(path, paint);//绘制路径

### 4.此时我们已经绘制好静态的图形了，接着需要让drag圆的圆心跟随我们手指触摸点来改变坐标，同时还要重绘才能改变效果 ###

	public boolean onTouchEvent(MotionEvent event) {
	    switch (event.getAction()) {
	    case MotionEvent.ACTION_DOWN:
	        dragCenter.set(event.getRawX(), event.getRawY());
	        break;
	    case MotionEvent.ACTION_MOVE:
	        dragCenter.set(event.getRawX(), event.getRawY());
	        break;
	    case MotionEvent.ACTION_UP:
	        break;
	    }
	    //坐标值改变还必须要重绘
	    invalidate();
	    return true;
	}

### 5.此时，drag圆的会随着手指触摸点的坐标改变而改变，但是y坐标有一定偏差，原因是我们获取y坐标用的是getRawY，是以屏幕左上角为原点，而真正绘制的时候是以画布(即当前View)左上角为原点，所以整个相差一个状态栏的高度，那么我们需要让画布向上移动一个状态栏的高度 ###

	//让画布向上移动一个状态栏的高度
	canvas.translate(0, -Utils.getStatusBarHeight(getResources()));

### 6.然后，drag圆圆心的坐标没问题了，但此时只有drag圆会移动，而2圆中间连接部分是死的，要让2圆连接部分也动态改变，那么需要我们去根据drag圆圆心坐标计算贝塞尔曲线的起点，控制点，终点的坐标 ###

	float xOffset = dragCenter.x - gooCenter.x;
	float yOffset = dragCenter.y - gooCenter.y;
	if(xOffset!=0){
	    lineK = yOffset/xOffset;
	}
	//动态计算drag圆的2点
	dragPoints = GeometryUtil.getIntersectionPoints(dragCenter, dragRadius, lineK);
	//动态计算goo圆的2点
	gooPoints = GeometryUtil.getIntersectionPoints(gooCenter, gooRadius, lineK);
	//动态计算控制点
	controlPoint = GeometryUtil.getPointByPercent(dragCenter, gooCenter,0.618f);

### 7.这样我们就实现了动态改变2圆连接部分，然后我们希望goo圆的半径能够随着2圆圆心距离的改变而变化，所以动态计算goo圆的半径 ###

	//动态计算goo圆的半径
	gooRadius = getGooRadius();

具体计算代码如下：

	private float maxDistance = 80f;//设定圆心最大距离为80
	/**
	 * 根据2圆圆心的距离来计算goo圆的半径
	 * @return
	 */
	private float getGooRadius(){
	    //获取圆心的距离
	    float distance = GeometryUtil.getDistanceBetween2Points(dragCenter, gooCenter);
	    //计算当前圆心距离占最大距离的百分比
	    float fraction = distance/maxDistance;
	    return GeometryUtil.evaluateValue(fraction,15f, 3f);
	}

### 8.随着我们的拖拽，2圆圆心距离会增大，如果超出最大值，则需要断开，断开的实现就是不绘制2圆连接部分即可，所以我们增加了标记isOutOfRange，如果为true，在onDraw方法中就不再去绘制2圆连接部分的贝塞尔曲线了 ###

	//获取2圆圆心的距离
	float distance = GeometryUtil.getDistanceBetween2Points(dragCenter, gooCenter);
	if(distance>maxDistance){
	    //意味着超过最大值，需要断掉
	    isOutOfRange = true;
	}

在onDraw方法中判断，不去绘制贝塞尔曲线连接部分：

	if(!isOutOfRange){
	    //如果没有超出范围，才去绘制贝塞尔曲线的连接部分
	    ...
	}

### 9.最后抬起手指的时候，如果2圆圆心距离没有超出范围则进行回弹，回弹使用ValueAnimator实现，这里自定义动画的逻辑是让drag圆的圆心不断向goo圆靠近，最后重叠 ###

	//如果抬起的时候没有超出范围，则弹回去
	final PointF startPointF = new PointF(dragCenter.x,dragCenter.y);
	ValueAnimator animator = ValueAnimator.ofFloat(1);
	animator.addUpdateListener(new AnimatorUpdateListener() {
	    @Override
	    public void onAnimationUpdate(ValueAnimator animator) {
	        float percent = animator.getAnimatedFraction();
	        PointF pointF = GeometryUtil.getPointByPercent(startPointF, gooCenter, percent);
	        //将得到的点设置给drag圆的圆心
	        dragCenter.set(pointF);
	        //再次重绘
	        invalidate();
	    }
	});
	animator.setInterpolator(new OvershootInterpolator(5));
	animator.setDuration(350);
	animator.start();

## 步骤1.将JitPack存储库添加到构建文件中 ##


将其添加到存储库末尾的根build.gradle中：


    	allprojects {
			repositories {
				...
				maven { url 'https://jitpack.io' }
			}
		}

## 步骤2.添加依赖项 ##


    dependencies {
	         implementation 'com.github.nangongyibin:Android_GooView:1.0.0'
	}


![](https://github.com/nangongyibin/Android_GooView/blob/master/ss.gif?raw=true)
