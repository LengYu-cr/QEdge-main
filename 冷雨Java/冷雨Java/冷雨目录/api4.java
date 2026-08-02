addItem("拓展脚本","tzjb");
List download1 = new ArrayList();
List download2 = new ArrayList();
List download3 = new ArrayList();
List download4 = new ArrayList();
List download5 = new ArrayList();
List download6 = new ArrayList();

public void getJavaList3() {
    download1.clear();
    download2.clear();
    download3.clear();
    download4.clear();
    download5.clear();
    download6.clear();
    try {
        File file = new File(RootPath);
        if (file != null && file.listFiles() != null) {
            for (File files : file.listFiles()) {
                String name = files.getName();
                if (!files.isDirectory() && name.startsWith("拓展-") && name.endsWith(".java")) {
                    download1.add(name.substring(3));
                    download2.add("冷雨");
                    download3.add("暂无描述");
                    download4.add("0.0.0");
                    download5.add(RootPath + name);
                    download6.add(name);
                }
            }
        }
    } catch (Exception e) {
        sendMsg(myUin,"获取失败"+e,1);
    }
}

// 优化后的展开动画
public void expandView(View view) {
    if (view.getVisibility() == View.VISIBLE) return;
    
    // 预先测量高度
    view.measure(View.MeasureSpec.makeMeasureSpec(((View)view.getParent()).getWidth(), View.MeasureSpec.EXACTLY),
                 View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED));
    final int targetHeight = view.getMeasuredHeight();
    
    view.getLayoutParams().height = 0;
    view.setVisibility(View.VISIBLE);
    
    Animation animation = new Animation() {
        protected void applyTransformation(float interpolatedTime, Transformation t) {
            // 使用线性插值，避免卡顿
            view.getLayoutParams().height = (int) (targetHeight * interpolatedTime);
            view.requestLayout();
        }
        
        public boolean willChangeBounds() {
            return true;
        }
    };
    animation.setDuration(200); // 稍微减少动画时间
    view.startAnimation(animation);
}

// 优化后的折叠动画
public void collapseView(View view) {
    if (view.getVisibility() != View.VISIBLE) return;
    
    final int initialHeight = view.getMeasuredHeight();
    
    Animation animation = new Animation() {
        protected void applyTransformation(float interpolatedTime, Transformation t) {
            if (interpolatedTime == 1) {
                view.setVisibility(View.GONE);
                view.getLayoutParams().height = 0;
            } else {
                view.getLayoutParams().height = initialHeight - (int) (initialHeight * interpolatedTime);
                view.requestLayout();
            }
        }
        
        public boolean willChangeBounds() {
            return true;
        }
    };
    animation.setDuration(200);
    view.startAnimation(animation);
}

public void 本地脚本() {
    final Activity Thisactixity=getNowActivity();
    Thisactixity.runOnUiThread(new Runnable() {
        public void run() {
            try {
                final LinearLayout l2 = new LinearLayout(Thisactixity);
                l2.setOrientation(LinearLayout.VERTICAL);
                l2.setBackground(getShape("#FFFFFF","#E3F2FD",0,20,255,true));

                TextView tt=new TextView(Thisactixity);
                tt.setText("📁 本地脚本列表");
                tt.setTextColor(Color.parseColor("#1976D2"));
                tt.setGravity(Gravity.CENTER_HORIZONTAL);
                tt.setTextSize(22);
                tt.setTypeface(null, Typeface.BOLD);
                tt.setPadding(0,20,0,10);
                l2.addView(tt);

                LinearLayout quickActionLayout = new LinearLayout(Thisactixity);
                quickActionLayout.setOrientation(LinearLayout.HORIZONTAL);
                quickActionLayout.setGravity(Gravity.CENTER_HORIZONTAL);
                quickActionLayout.setPadding(10,0,10,10);

                final Button loadAllBtn = new Button(Thisactixity);
                loadAllBtn.setText("一键加载全部");
                loadAllBtn.setTextColor(Color.parseColor("#FFFFFF"));
                loadAllBtn.setBackground(getShape("#2196F3","#1976D2",0,15,255,false));
                loadAllBtn.setPadding(20,10,20,10);
                loadAllBtn.setTextSize(14);

                final Button autoAllBtn = new Button(Thisactixity);
                autoAllBtn.setText("一键自启动全部");
                autoAllBtn.setTextColor(Color.parseColor("#FFFFFF"));
                autoAllBtn.setBackground(getShape("#4CAF50","#388E3C",0,15,255,false));
                autoAllBtn.setPadding(20,10,20,10);
                autoAllBtn.setTextSize(14);

                LinearLayout.LayoutParams btnParams = new LinearLayout.LayoutParams(
                    0, LinearLayout.LayoutParams.WRAP_CONTENT, 1.0f
                );
                btnParams.setMargins(5,0,5,0);
                loadAllBtn.setLayoutParams(btnParams);
                autoAllBtn.setLayoutParams(btnParams);
                quickActionLayout.addView(loadAllBtn);
                quickActionLayout.addView(autoAllBtn);
                l2.addView(quickActionLayout);

                final ScrollView mScrollView = new ScrollView(Thisactixity);
                mScrollView.setPadding(10,10,10,10);
                final LinearLayout l5 = new LinearLayout(Thisactixity);
                l5.setOrientation(LinearLayout.VERTICAL);
                mScrollView.addView(l5);
                l2.addView(mScrollView);

                final ProgressBar yq = new ProgressBar(Thisactixity);
                l5.addView(yq);

                final Dialog dialog = new Dialog(Thisactixity);
                dialog.getWindow().clearFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND);
                dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
                dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
                dialog.setContentView(l2);
                dialog.setCancelable(true);

                WindowManager wm = (WindowManager) Thisactixity.getSystemService(Context.WINDOW_SERVICE);
                int height = wm.getDefaultDisplay().getHeight();
                int width = wm.getDefaultDisplay().getWidth();
                height=height/2+height/6;
                width=width/2+width/4;
                l2.getLayoutParams().height =height;
                l2.getLayoutParams().width =width;
                dialog.show();

                new Thread(new Runnable() {
                    public void run() {
                        getJavaList3();
                        l5.post(new Runnable() {
                            public void run() {
                                yq.setVisibility(View.GONE);
                            }
                        });

                        for(int i=0;i<download1.size();i++){
                            final int k=i;
                            Thread.sleep(100);
                            l5.post(new Runnable() {
                                public void run(){
                                    try {
                                        final String name = (String) download1.get(k);
                                        final String desc = (String) download3.get(k);
                                        final String path = (String) download5.get(k);
                                        final String fileName = (String) download6.get(k);

                                        final LinearLayout l3 = new LinearLayout(Thisactixity);
                                        l3.setOrientation(LinearLayout.VERTICAL);
                                        l3.setPadding(15,12,15,12);
                                        l3.setBackground(getShape("#FFFFFF","#E3F2FD",1,12,255,false));
                                        LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(
                                            LinearLayout.LayoutParams.MATCH_PARENT,
                                            LinearLayout.LayoutParams.WRAP_CONTENT
                                        );
                                        p.setMargins(0,0,0,8);
                                        l3.setLayoutParams(p);

                                        // 标题 —— 已去掉版本号 (0.0.0)
                                        TextView t1 = new TextView(Thisactixity);
                                        t1.setText(name);
                                        t1.setTextColor(Color.parseColor("#1976D2"));
                                        t1.setTextSize(18);
                                        t1.setTypeface(null,Typeface.BOLD);
                                        l3.addView(t1);

                                        final LinearLayout l4 = new LinearLayout(Thisactixity);
                                        l4.setOrientation(LinearLayout.VERTICAL);
                                        l4.setPadding(0,10,0,0);
                                        l4.setVisibility(View.GONE);

                                        TextView t4 = new TextView(Thisactixity);
                                        t4.setText("描述:\n"+desc);
                                        t4.setTextSize(12);
                                        t4.setTextColor(Color.parseColor("#666666"));
                                        l4.addView(t4);

                                        LinearLayout l6 = new LinearLayout(Thisactixity);
                                        l6.setOrientation(LinearLayout.HORIZONTAL);
                                        l6.setPadding(0,10,0,0);

                                        final Button b1 = new Button(Thisactixity);
                                        boolean loaded = hasLoaded(fileName);
                                        b1.setText(loaded ? "已加载" : "加载");
                                        b1.setPadding(15,8,15,8);
                                        b1.setTextSize(14);
                                        if(loaded){
                                            b1.setBackground(getShape("#9E9E9E","#757575",0,8,255,false));
                                        }else{
                                            b1.setBackground(getShape("#2196F3","#1976D2",0,8,255,false));
                                        }
                                        b1.setTextColor(Color.parseColor("#FFFFFF"));

                                        final Button b3 = new Button(Thisactixity);
                                        boolean auto = isAutoLoad(path);
                                        b3.setText(auto ? "关闭自动加载" : "开启自动加载");
                                        b3.setPadding(15,8,15,8);
                                        b3.setTextSize(14);
                                        if(auto){
                                            b3.setBackground(getShape("#FF5722","#D84315",0,8,255,false));
                                        }else{
                                            b3.setBackground(getShape("#4CAF50","#388E3C",0,8,255,false));
                                        }
                                        b3.setTextColor(Color.parseColor("#FFFFFF"));

                                        TextView sp1 = new TextView(Thisactixity);
                                        sp1.setLayoutParams(new LinearLayout.LayoutParams(0,LinearLayout.LayoutParams.WRAP_CONTENT,1));
                                        TextView sp2 = new TextView(Thisactixity);
                                        sp2.setLayoutParams(new LinearLayout.LayoutParams(0,LinearLayout.LayoutParams.WRAP_CONTENT,1));

                                        l6.addView(sp1);
                                        l6.addView(b1);
                                        l6.addView(sp2);
                                        l6.addView(b3);
                                        l4.addView(l6);
                                        l3.addView(l4);

                                        // 使用标志位防止重复点击
                                        final boolean[] isAnimating = {false};
                                        
                                        // 点击展开/折叠 —— 优化卡顿
                                        l3.setOnClickListener(new View.OnClickListener() {
                                            public void onClick(View v) {
                                                if(isAnimating[0]) return; // 正在动画中，忽略点击
                                                
                                                isAnimating[0] = true;
                                                
                                                // 延迟重置标志位
                                                v.postDelayed(new Runnable() {
                                                    public void run() {
                                                        isAnimating[0] = false;
                                                    }
                                                }, 250);
                                                
                                                if(l4.getVisibility() == View.GONE){
                                                    expandView(l4);
                                                }else{
                                                    collapseView(l4);
                                                }
                                            }
                                        });

                                        // 加载按钮
                                        b1.setOnClickListener(new View.OnClickListener() {
                                            public void onClick(View v) {
                                                try {
                                                    if(loaded){
                                                        Toast("已经加载成功了，别点了");
                                                    }else{
                                                        loadJava(path);
                                                        json_java.put(fileName,1);
                                                        saveText(b1,"已加载");
                                                        b1.setBackground(getShape("#9E9E9E","#757575",0,8,255,false));
                                                        Toast("加载成功");
                                                    }
                                                } catch(Exception e){
                                                    Toast("加载出错\n"+e);
                                                }
                                            }
                                        });

                                        // 自动加载按钮
                                        b3.setOnClickListener(new View.OnClickListener() {
                                            public void onClick(View v) {
                                                if(isAutoLoad(path)){
                                                    DeleteJava(path);
                                                    Toast("取消自动加载成功");
                                                    saveText(b3,"开启自动加载");
                                                    b3.setBackground(getShape("#4CAF50","#388E3C",0,8,255,false));
                                                }else{
                                                    AddJava(path);
                                                    Toast("开启自动加载成功，下次生效");
                                                    saveText(b3,"关闭自动加载");
                                                    b3.setBackground(getShape("#FF5722","#D84315",0,8,255,false));
                                                }
                                            }
                                        });

                                        l5.addView(l3);

                                    }catch(Exception e){}
                                }
                            });
                        }

                        // 一键加载
                        loadAllBtn.setOnClickListener(new View.OnClickListener() {
                            public void onClick(View v) {
                                new Thread(new Runnable() {
                                    public void run() {
                                        int c=0;
                                        Toast("开始加载所有拓展脚本");
                                        for(int i=0;i<download1.size();i++){
                                            if(!hasLoaded((String)download6.get(i))){
                                                try {
                                                    loadJava((String)download5.get(i));
                                                    json_java.put((String)download6.get(i),1);
                                                    c++;
                                                }catch (Exception e){}
                                            }
                                        }
                                        final int count = c;
                                        Thisactixity.runOnUiThread(new Runnable() {
                                            public void run() {
                                                Toast("一键加载完成："+count+" 个");
                                            }
                                        });
                                    }
                                }).start();
                            }
                        });

                        // 一键自启
                        autoAllBtn.setOnClickListener(new View.OnClickListener() {
                            public void onClick(View v) {
                                new Thread(new Runnable() {
                                    public void run() {
                                        int c=0;
                                        Toast("开始设置全部自启动");
                                        for(int i=0;i<download1.size();i++){
                                            if(!isAutoLoad((String)download5.get(i))){
                                                try {
                                                    AddJava((String)download5.get(i));
                                                    c++;
                                                }catch (Exception e){}
                                            }
                                        }
                                        final int count = c;
                                        Thisactixity.runOnUiThread(new Runnable() {
                                            public void run() {
                                                Toast("自启动设置完成："+count+" 个");
                                            }
                                        });
                                    }
                                }).start();
                            }
                        });
                    }
                }).start();

            }catch(Exception e){
                Toast("界面创建失败");
            }
        }
    });
}
public void saveText(Button b,String text) {
    Activity Thisactixity=getNowActivity();
    Thisactixity.runOnUiThread(new Runnable() {
        public void run() {
            b.setText(text);
        }
    });
}

public Bitmap compressBitmap(Bitmap bia, int maxWidth, int maxHeight) {
    int originalWidth = bia.getWidth();
    int originalHeight = bia.getHeight();
    if (originalWidth <= maxWidth && originalHeight <= maxHeight) {
        return bia;
    }
    float scale = Math.min((float) maxWidth / originalWidth, (float) maxHeight / originalHeight);
    Matrix matrix = new Matrix();
    matrix.postScale(scale, scale);
    Bitmap compressedBitmap = Bitmap.createBitmap(bia, 0, 0, originalWidth, originalHeight, matrix, true);
    return compressedBitmap;
}
public Bitmap getpicshape(Bitmap originalBitmap,int radius,boolean shadow) {
    Bitmap roundedBitmap = Bitmap.createBitmap(originalBitmap.getWidth(), originalBitmap.getHeight(), Bitmap.Config.ARGB_8888);
    Canvas canvas = new Canvas(roundedBitmap);
    Path path = new Path();
    float[] radii = {
        radius, radius, radius, radius, radius, radius, radius, radius
    };
    RectF rect = new RectF(0, 0, originalBitmap.getWidth(), originalBitmap.getHeight());
    path.addRoundRect(rect, radii, Path.Direction.CW);
    canvas.clipPath(path);
    canvas.drawBitmap(originalBitmap, 0, 0, null);
    if(shadow) {
        return addShadowToBitmap(roundedBitmap);
    }
    return roundedBitmap;
}
public Bitmap addShadowToBitmap(Bitmap originalBitmap) {
    Bitmap shadowBitmap = Bitmap.createBitmap(originalBitmap.getWidth(), originalBitmap.getHeight(), Bitmap.Config.ARGB_8888);
    Canvas canvas = new Canvas(shadowBitmap);
    Paint paint = new Paint();
    paint.setColor(Color.BLACK);
    paint.setMaskFilter(new BlurMaskFilter(25, BlurMaskFilter.Blur.NORMAL));
    canvas.drawBitmap(originalBitmap, 0, 0, paint);
    return shadowBitmap;
}
public GradientDrawable getShape(String color1, String color2, int size1, int size2, int tm,boolean pd) {
    GradientDrawable shape;
    if(pd) {
        int[] colors = {
            Color.parseColor(color1), Color.parseColor(color2)
        };
        shape = new GradientDrawable(GradientDrawable.Orientation.TOP_BOTTOM, colors);
    }
    else {
        shape=new GradientDrawable();
        shape.setColor(Color.parseColor(color1));
    }
    shape.setStroke(size1, Color.parseColor(color2));
    shape.setCornerRadius(size2);
    shape.setAlpha(tm);
    shape.setShape(GradientDrawable.RECTANGLE);
    return shape;
}
public static GradientDrawable getShape(String color1,String color2,int size1,int size2)
{
    GradientDrawable shape=new GradientDrawable();
    shape.setColor(Color.parseColor(color1));
    shape.setStroke(size1,Color.parseColor(color2));
    shape.setCornerRadius(size2);
    shape.setAlpha(230);
    shape.setShape(GradientDrawable.RECTANGLE);
    return shape;
}
public int payList(String a) {
    boolean ok = false;
    int result = -1;
    Activity ThisActivity = getNowActivity();
    String okk = DownloadToFile("https://sfile.chatglm.cn/chatglm4/b247b66d-5d06-4934-ad8a-b3b3579d8ad7.jpg", ColdRainPath + "/图片/赞助背景图");
    String path = ColdRainPath + "/图片/赞助背景图";
    Bitmap myjpg;
    if (okk.equals("成功"))
    myjpg = compressBitmap(BitmapFactory.decodeFile(path), 1000, 1000);
    ThisActivity.runOnUiThread(new Runnable() {
        public void run() {
            LinearLayout mainLayout = new LinearLayout(ThisActivity);
            mainLayout.setOrientation(LinearLayout.VERTICAL);
            mainLayout.setPadding(30, 30, 30, 20);
            mainLayout.setBackground(getShape("#FFFFFF", "#FFE4E6", 2, 25, 255, true));
            TextView title = new TextView(ThisActivity);
            title.setText("感谢支持～你真好看～\\( ͯω ͯ)/");
            title.setTextColor(Color.parseColor("#E91E63"));
            title.setTextSize(18);
            title.setTypeface(null, Typeface.BOLD);
            title.setGravity(Gravity.CENTER);
            LinearLayout.LayoutParams titleParams = new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
            );
            titleParams.setMargins(0, 0, 0, 20);
            title.setLayoutParams(titleParams);
            ImageView imageView = new ImageView(ThisActivity);
            if (okk.equals("成功")) {
                imageView.setImageBitmap(getpicshape(myjpg, 20, true));
            }
            imageView.setScaleType(ImageView.ScaleType.CENTER_CROP);
            LinearLayout.LayoutParams imageParams = new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            dpToPx(ThisActivity, 150)
            );
            imageParams.setMargins(0, 0, 0, 20);
            imageView.setLayoutParams(imageParams);
            imageView.setOnClickListener(new View.OnClickListener() {
                public void onClick(View v) {
                    int i = 随机数(1, 5);
                    String[] messages = {
                        "为什莫要点我嘞～",
                        "别点啦！歇歇吧",
                        "停手吧，求你啦",
                        "点我干嘛～你要看的在下面呢！",
                        "不许点了！"
                    };
                    Toast(messages[i-1]);
                }
            }
            );
            TextView descriptionText = new TextView(ThisActivity);
            descriptionText.setText("1. 赞助用户将会获得自动更新权限，非赞助用户的只能手动更新，并且不是大更新压缩包不会外发\n\n" +
            "2. 赞助是一种支持，赞不赞助是你的权利，赞助会用于服务器续费等等\n\n" +
            "3. 请赞助时配上QQ号或截图发送给作者任意账号\n\n" +
            "4. 注意：赞助后无法退款，因为被作者吃掉啦！\n\n" +
            "5. 目前不支持支付宝赞助");
            descriptionText.setTextSize(14);
            descriptionText.setTextColor(Color.parseColor("#666666"));
            descriptionText.setLineSpacing(1.2f, 1.2f);
            descriptionText.setPadding(20, 15, 20, 25);
            descriptionText.setBackground(getShape("#FFF5F7", "#FFE4E6", 1, 15, 255, false));
            LinearLayout.LayoutParams descParams = new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
            );
            descParams.setMargins(0, 0, 0, 20);
            descriptionText.setLayoutParams(descParams);
            TextView serverHint = new TextView(ThisActivity);
            serverHint.setText("服务器费用：小贵(但没以前贵了捏～) | 当前总赞助：每月＜30r");
            serverHint.setTextColor(Color.parseColor("#FF6B6B"));
            serverHint.setTextSize(12);
            serverHint.setGravity(Gravity.CENTER);
            serverHint.setTypeface(null, Typeface.BOLD);
            serverHint.setPadding(0, 0, 0, 15);
            serverHint.setLayoutParams(new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
            ));
            TextView optionTitle = new TextView(ThisActivity);
            optionTitle.setText("选择赞助方式");
            optionTitle.setTextColor(Color.parseColor("#E91E63"));
            optionTitle.setTextSize(16);
            optionTitle.setTypeface(null, Typeface.BOLD);
            optionTitle.setGravity(Gravity.CENTER);
            optionTitle.setPadding(0, 0, 0, 10);
            optionTitle.setLayoutParams(new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
            ));
            Button wechatBtn = new Button(ThisActivity);
            wechatBtn.setText("💖 微信/QQ 赞助");
            wechatBtn.setTextColor(Color.parseColor("#FFFFFF"));
            wechatBtn.setTextSize(16);
            wechatBtn.setTypeface(null, Typeface.BOLD);
            wechatBtn.setBackground(getShape("#E91E63", "#C2185B", 0, 25, 255, false));
            wechatBtn.setPadding(0, 20, 0, 20);
            LinearLayout.LayoutParams btnParams = new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
            );
            btnParams.setMargins(0, 0, 0, 15);
            wechatBtn.setLayoutParams(btnParams);
            LinearLayout buttonLayout = new LinearLayout(ThisActivity);
            buttonLayout.setOrientation(LinearLayout.HORIZONTAL);
            buttonLayout.setGravity(Gravity.CENTER_HORIZONTAL);
            LinearLayout.LayoutParams buttonLayoutParams = new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
            );
            buttonLayout.setLayoutParams(buttonLayoutParams);
            Button closeBtn = new Button(ThisActivity);
            closeBtn.setText("关闭");
            closeBtn.setTextColor(Color.parseColor("#666666"));
            closeBtn.setBackground(getShape("#F5F5F5", "#E0E0E0", 1, 20, 255, false));
            closeBtn.setPadding(40, 15, 40, 15);
            closeBtn.setTextSize(14);
            LinearLayout.LayoutParams closeParams = new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.WRAP_CONTENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
            );
            closeBtn.setLayoutParams(closeParams);
            buttonLayout.addView(closeBtn);
            mainLayout.addView(title);
            mainLayout.addView(imageView);
            mainLayout.addView(descriptionText);
            mainLayout.addView(serverHint);
            mainLayout.addView(optionTitle);
            mainLayout.addView(wechatBtn);
            mainLayout.addView(buttonLayout);
            Dialog dialog = new Dialog(ThisActivity);
            dialog.setContentView(mainLayout);
            dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
            dialog.setCancelable(true);
            WindowManager wm = (WindowManager) ThisActivity.getSystemService(Context.WINDOW_SERVICE);
            Display display = wm.getDefaultDisplay();
            int width = (int) (display.getWidth() * 0.85);
            WindowManager.LayoutParams layoutParams = new WindowManager.LayoutParams();
            layoutParams.copyFrom(dialog.getWindow().getAttributes());
            layoutParams.width = width;
            layoutParams.height = WindowManager.LayoutParams.WRAP_CONTENT;
            dialog.getWindow().setAttributes(layoutParams);
            boolean[] dialogOk = {
                false
            };
            int[] dialogResult = {
                -1
            };
            wechatBtn.setOnClickListener(new View.OnClickListener() {
                public void onClick(View v) {
                    dialogResult[0] = 0;
                    dialogOk[0] = true;
                    dialog.dismiss();
                }
            }
            );
            closeBtn.setOnClickListener(new View.OnClickListener() {
                public void onClick(View v) {
                    dialogOk[0] = true;
                    dialog.dismiss();
                }
            }
            );
            dialog.setOnCancelListener(new DialogInterface.OnCancelListener() {
                public void onCancel(DialogInterface dialog) {
                    dialogOk[0] = true;
                }
            }
            );
            dialog.show();
            dialog.setOnDismissListener(new DialogInterface.OnDismissListener() {
                public void onDismiss(DialogInterface dialog) {
                    synchronized (dialogOk) {
                        dialogOk[0] = true;
                        dialogOk.notify();
                    }
                }
            }
            );
            new Thread(new Runnable() {
                public void run() {
                    synchronized (dialogOk) {
                        while (!dialogOk[0]) {
                            try {
                                dialogOk.wait();
                            }
                            catch (InterruptedException e) {
                                e.printStackTrace();
                            }
                        }
                    }
                    ok = true;
                    result = dialogResult[0];
                }
            }
            ).start();
        }
    }
    );
    while (!ok) {
        try {
            Thread.sleep(500);
        }
        catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
    return result;
}
public void payList() {
    new Thread(new Runnable() {
        public void run() {
            int i = payList("感谢支持！");
            if (i == 0) {
                Toast("正在打开赞助页面...");
                Activity activity = getNowActivity();
                Intent intent = new Intent(Intent.ACTION_VIEW);
                intent.setData(Uri.parse("https://api.yuafeng.cn/ly/zan/"));
                activity.startActivity(intent);
            }
        }
    }
    ).start();
}
import android.renderscript.Allocation;
import android.renderscript.Element;
import android.renderscript.RenderScript;
import android.renderscript.ScriptIntrinsicBlur;
import android.renderscript.*;
public static Bitmap PortraitToLandscape(Bitmap originalBitmap) {
    //竖屏转横屏
    int width = originalBitmap.getWidth();
    int height = originalBitmap.getHeight();
    int height2=height/4.5;
    return Bitmap.createBitmap(originalBitmap,0,height/2,width,height2);
}
public static Bitmap blurBitmap(Context context, Bitmap bitmap, float radius) {
    Bitmap outBitmap = Bitmap.createBitmap(bitmap.getWidth(), bitmap.getHeight(), bitmap.getConfig());
    RenderScript rs = RenderScript.create(context);
    ScriptIntrinsicBlur blurScript = ScriptIntrinsicBlur.create(rs, Element.U8_4(rs));
    Allocation allIn = Allocation.createFromBitmap(rs, bitmap);
    Allocation allOut = Allocation.createFromBitmap(rs, outBitmap);
    blurScript.setRadius(radius);
    blurScript.setInput(allIn);
    blurScript.forEach(allOut);
    allOut.copyTo(outBitmap);
    rs.destroy();
    return outBitmap;
}
public boolean isAutoLoad(String path) {
    File file = new File(path);
    if(file.exists()) {
        String a = json_java2.optString(path);
        if(a.equals("1")) {
            return true;
        }
        else if(a.equals("0")) {
            return false;
        }
        else {
            int a = 读("0","冷雨Java",path);
            if(a==1) return true;
            else return false;
        }
    }
    else {
        Toast(path + "已被删除，将不再自动加载，防止报错！");
        写("0","冷雨Java",path,0);
        //文件被删除，取消自动加载
        return false;
    }
}
public boolean hasLoaded(String name) {
    String a = json_java.optString(name);
    if(a.equals("1"))return true;
    else return false;
}
public AddJava(String path) {
    json_java2.put(path,1);
    写("0","冷雨java",path,1);
}
public DeleteJava(String path) {
    json_java2.put(path,0);
    清除("0","冷雨java",path);
}
JSONObject json_java=new JSONObject();
JSONObject json_java2=new JSONObject();

public void loadAllJavas(File[] files) {
new Thread(new Runnable() {
    public void run() {
    
    for(File files : files) {
         String name = files.getName();
             if(!files.isDirectory() && name.startsWith("拓展-") && isAutoLoad(RootPath+name)) {
                 loadJava(RootPath + name);
                 json_java.put(name, 1);
         }
    }
}
}).start();
}

public void downloadAllJavas(String qun,int mtype) {
new Thread(new Runnable() {
    public void run() {
        String url = get("https://api.yuafeng.cn/ly/java/?action=script_list");
        if(url.isEmpty() || url.equals("访问网页失败")) {
             Toast("下载拓展脚本失败，请到官方群反馈");
        } else {
             JSONObject json = new JSONObject(url);
             int code = json.getInt("code");
             int count = json.getInt("count");
             if(code == 0) {
                 if(count == 0) {
                     Toast("下载拓展脚本失败，官方服务器已删除所有拓展脚本");
                 }
                 JSONArray scripts = json.getJSONArray("scripts");
                 String result = "已下载以下脚本:\n";
                 for(int i = 0; i < scripts.length(); i++ ) {
                     JSONObject item = scripts.getJSONObject(i);
                     String name = item.getString("script_name");
                     String download_url = item.getString("download_url");
                     String file_name = item.getString("file_name");
                     
                     String tf = DownloadToFile(download_url, RootPath + file_name);
                     
                     if(tf.equals("成功")) {
                         result += (i+1) + "、" + name + " -> " + file_name + "\n";
                     }
                 }
                 sendMsg(qun, result, mtype);
             }
        }
    }
}).start();
}
new Thread(new Runnable() {
    public void run() {
        try {
            File file = new File(RootPath);
            
            File[] files = file.listFiles();
            
            int count = 0;
            
            for(File files : files) {
                String name = files.getName();
                if(!files.isDirectory() && name.startsWith("拓展-") && name.endsWith(".java") ) {
                     count ++;
                 }
            }
            
            if(count == 0) {
                Toast("检测到本地没有拓展脚本列表，正在尝试下载拓展脚本列表");
                downloadAllJavas(myUin,1);
            } else {
                loadAllJavas(files);
            }
            
        }
        catch (JSONException e) {
            sendMsg(myUin,"加载拓展失败"+e,1);
        }
    }
}
).start();


if(加载次数==0) {
    new Thread(new Runnable() {
        public void run() {
            put(ColdRainPath+"data/0/冷雨Java.txt","");
            Toast("为解决部分指令未找到问题，现已清除本地自动加载拓展Java数据，需要重新开启拓展Java的自动加载，后续将不再清除数据");
            本地脚本();
            Toast("检测到首次加载本Java，请先选择你要加载的功能吧！");
        }
    }
    ).start();
}
写("0","加载次数","冷雨Java",加载次数+1);
addItem("我要赞助","wyzz");
addItem("搜索音乐","music");
public void searchMusic(String qun, int type) {
    String yyms = 读(ColdRainPath + "data/" + qun + "点歌模式.txt");
    if (yyms.equals("")) yyms = "卡片";
    Activity ThisActivity = getNowActivity();
    ThisActivity.runOnUiThread(new Runnable() {
        public void run() {
            LinearLayout mainLayout = new LinearLayout(ThisActivity);
            mainLayout.setOrientation(LinearLayout.VERTICAL);
            mainLayout.setPadding(40, 30, 40, 30);
            mainLayout.setBackground(getShape("#FFFFFF", "#E3F2FD", 2, 25, 255, true));
            TextView title = new TextView(ThisActivity);
            title.setText("搜索歌曲 (QQ音乐)");
            title.setTextColor(Color.parseColor("#1976D2"));
            title.setTextSize(20);
            title.setTypeface(null, Typeface.BOLD);
            title.setGravity(Gravity.CENTER);
            LinearLayout.LayoutParams titleParams = new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
            );
            titleParams.setMargins(0, 0, 0, 30);
            title.setLayoutParams(titleParams);
            TextView inputHint = new TextView(ThisActivity);
            inputHint.setText("输入歌手/歌曲名");
            inputHint.setTextColor(Color.parseColor("#424242"));
            inputHint.setTextSize(14);
            inputHint.setLayoutParams(new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
            ));
            EditText editText = new EditText(ThisActivity);
            editText.setHint("请输入歌曲或歌手名称...");
            editText.setText("");
            editText.setBackground(getShape("#FFFFFF", "#BBDEFB", 2, 15, 255, false));
            editText.setPadding(20, 15, 20, 15);
            editText.setTextColor(Color.parseColor("#212121"));
            editText.setHintTextColor(Color.parseColor("#9E9E9E"));
            editText.setTextSize(16);
            LinearLayout.LayoutParams editParams = new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
            );
            editParams.setMargins(0, 10, 0, 25);
            editText.setLayoutParams(editParams);
            TextView modeTitle = new TextView(ThisActivity);
            modeTitle.setText("选择发送模式");
            modeTitle.setTextColor(Color.parseColor("#424242"));
            modeTitle.setTextSize(14);
            modeTitle.setTypeface(null, Typeface.BOLD);
            LinearLayout.LayoutParams modeTitleParams = new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
            );
            modeTitleParams.setMargins(0, 0, 0, 10);
            modeTitle.setLayoutParams(modeTitleParams);
            GridLayout modeGrid = new GridLayout(ThisActivity);
            modeGrid.setColumnCount(3);
            modeGrid.setRowCount(3);
            LinearLayout.LayoutParams gridParams = new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
            );
            gridParams.setMargins(0, 0, 0, 25);
            modeGrid.setLayoutParams(gridParams);
            String[] modes = {
                "空间", "卡片", "语音", "链接", "下载", "文件", "播放"
            };
            String[] modeDescriptions = {
                "发送到QQ空间",
                "发送音乐卡片",
                "发送语音消息",
                "发送音乐链接",
                "下载音乐文件",
                "发送文件格式",
                "直接播放音乐"
            };
            String[] currentMode = {
                yyms
            };
            for (int i = 0;
            i < modes.length;
            i++) {
                String mode = modes[i];
                String description = modeDescriptions[i];
                Button modeBtn = new Button(ThisActivity);
                modeBtn.setText(mode);
                modeBtn.setTextSize(12);
                modeBtn.setPadding(5, 10, 5, 10);
                modeBtn.setGravity(Gravity.CENTER);
                GridLayout.LayoutParams btnParams = new GridLayout.LayoutParams();
                btnParams.width = 0;
                btnParams.height = GridLayout.LayoutParams.WRAP_CONTENT;
                btnParams.columnSpec = GridLayout.spec(i % 3, 1f);
                btnParams.rowSpec = GridLayout.spec(i / 3);
                btnParams.setMargins(5, 5, 5, 5);
                modeBtn.setLayoutParams(btnParams);
                if (mode.equals(yyms)) {
                    modeBtn.setTextColor(Color.parseColor("#FFFFFF"));
                    modeBtn.setBackground(getShape("#2196F3", "#1976D2", 0, 15, 255, false));
                    modeBtn.setTypeface(null, Typeface.BOLD);
                }
                else {
                    modeBtn.setTextColor(Color.parseColor("#666666"));
                    modeBtn.setBackground(getShape("#F5F5F5", "#E0E0E0", 1, 15, 255, false));
                }
                modeBtn.setOnClickListener(new View.OnClickListener() {
                    public void onClick(View v) {
                        currentMode[0] = mode;
                        for (int j = 0;
                        j < modeGrid.getChildCount();
                        j++) {
                            Button btn = (Button) modeGrid.getChildAt(j);
                            if (btn.getText().equals(mode)) {
                                btn.setTextColor(Color.parseColor("#FFFFFF"));
                                btn.setBackground(getShape("#2196F3", "#1976D2", 0, 15, 255, false));
                                btn.setTypeface(null, Typeface.BOLD);
                            }
                            else {
                                btn.setTextColor(Color.parseColor("#666666"));
                                btn.setBackground(getShape("#F5F5F5", "#E0E0E0", 1, 15, 255, false));
                                btn.setTypeface(null, Typeface.NORMAL);
                            }
                        }
                        Toast( "已选择: " + description);
                    }
                }
                );
                modeGrid.addView(modeBtn);
            }
            TextView currentModeText = new TextView(ThisActivity);
            currentModeText.setText("当前群内模式: " + yyms);
            currentModeText.setTextColor(Color.parseColor("#666666"));
            currentModeText.setTextSize(12);
            currentModeText.setGravity(Gravity.CENTER);
            LinearLayout.LayoutParams currentModeParams = new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
            );
            currentModeParams.setMargins(0, 0, 0, 20);
            currentModeText.setLayoutParams(currentModeParams);
            LinearLayout buttonLayout = new LinearLayout(ThisActivity);
            buttonLayout.setOrientation(LinearLayout.HORIZONTAL);
            buttonLayout.setGravity(Gravity.CENTER_HORIZONTAL);
            LinearLayout.LayoutParams buttonLayoutParams = new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
            );
            buttonLayoutParams.setMargins(0, 10, 0, 0);
            buttonLayout.setLayoutParams(buttonLayoutParams);
            Button cancelBtn = new Button(ThisActivity);
            cancelBtn.setText("取消");
            cancelBtn.setTextColor(Color.parseColor("#666666"));
            cancelBtn.setBackground(getShape("#F5F5F5", "#E0E0E0", 1, 20, 255, false));
            cancelBtn.setPadding(40, 15, 40, 15);
            cancelBtn.setTextSize(16);
            LinearLayout.LayoutParams cancelParams = new LinearLayout.LayoutParams(
            0,
            LinearLayout.LayoutParams.WRAP_CONTENT,
            1.0f
            );
            cancelParams.setMargins(0, 0, 10, 0);
            cancelBtn.setLayoutParams(cancelParams);
            Button confirmBtn = new Button(ThisActivity);
            confirmBtn.setText("搜索");
            confirmBtn.setTextColor(Color.parseColor("#FFFFFF"));
            confirmBtn.setBackground(getShape("#2196F3", "#1976D2", 0, 20, 255, false));
            confirmBtn.setPadding(40, 15, 40, 15);
            confirmBtn.setTextSize(16);
            LinearLayout.LayoutParams confirmParams = new LinearLayout.LayoutParams(
            0,
            LinearLayout.LayoutParams.WRAP_CONTENT,
            1.0f
            );
            confirmParams.setMargins(10, 0, 0, 0);
            confirmBtn.setLayoutParams(confirmParams);
            buttonLayout.addView(cancelBtn);
            buttonLayout.addView(confirmBtn);
            mainLayout.addView(title);
            mainLayout.addView(inputHint);
            mainLayout.addView(editText);
            mainLayout.addView(modeTitle);
            mainLayout.addView(modeGrid);
            mainLayout.addView(currentModeText);
            mainLayout.addView(buttonLayout);
            Dialog dialog = new Dialog(ThisActivity);
            dialog.setContentView(mainLayout);
            dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
            dialog.setCancelable(true);
            WindowManager wm = (WindowManager) ThisActivity.getSystemService(Context.WINDOW_SERVICE);
            Display display = wm.getDefaultDisplay();
            int width = (int) (display.getWidth() * 0.90);
            WindowManager.LayoutParams layoutParams = new WindowManager.LayoutParams();
            layoutParams.copyFrom(dialog.getWindow().getAttributes());
            layoutParams.width = width;
            layoutParams.height = WindowManager.LayoutParams.WRAP_CONTENT;
            dialog.getWindow().setAttributes(layoutParams);
            cancelBtn.setOnClickListener(new View.OnClickListener() {
                public void onClick(View v) {
                    dialog.dismiss();
                }
            }
            );
            confirmBtn.setOnClickListener(new View.OnClickListener() {
                public void onClick(View v) {
                    String searchText = editText.getText().toString().trim();
                    if (!searchText.equals("")) {
                        //写(ColdRainPath + "data/" + qun + "点歌模式.txt", currentMode[0]);
                        searchMusic(qun, searchText, currentMode[0], type);
                    }
                    else {
                        Toast.makeText(ThisActivity, "请输入搜索内容", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    dialog.dismiss();
                }
            }
            );
            editText.requestFocus();
            dialog.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE);
            dialog.show();
        }
    }
    );
}
public void searchMusic(String qun, String name, String msyy, int type) {
    Activity Thisactixity = getNowActivity();
    Thisactixity.runOnUiThread(new Runnable() {
        public void run() {
            try {
                Dialog dialog = new Dialog(Thisactixity);
                
                // 创建布局
                final LinearLayout l2 = new LinearLayout(Thisactixity);
                l2.setOrientation(LinearLayout.VERTICAL);
                l2.setBackground(getShape("#FDFFFF", "#D9FFFF", 0, 20, 255, true));
                
                // 标题
                TextView tt = new TextView(Thisactixity);
                tt.setText("搜索列表(支持付费歌曲)");
                tt.setTextColor(Color.parseColor("#111111"));
                tt.setGravity(Gravity.CENTER_HORIZONTAL);
                tt.setTextSize(25);
                l2.addView(tt);
                
                // 滚动视图
                final ScrollView mScrollView = new ScrollView(Thisactixity);
                mScrollView.setPadding(10, 10, 10, 10);
                
                // 内容容器
                final LinearLayout l5 = new LinearLayout(Thisactixity);
                l5.setOrientation(LinearLayout.VERTICAL);
                
                // 进度条
                final ProgressBar yq = new ProgressBar(Thisactixity);
                l5.addView(yq);
                
                mScrollView.addView(l5);
                l2.addView(mScrollView);
                
                // 配置对话框
                dialog.getWindow().clearFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND);
                dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
                dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
                dialog.setContentView(l2);
                dialog.setCancelable(true);
                
                // 设置对话框大小
                WindowManager wm = (WindowManager) Thisactixity.getSystemService(Context.WINDOW_SERVICE);
                int height = wm.getDefaultDisplay().getHeight();
                int width = wm.getDefaultDisplay().getWidth();
                height = height/2 + height/6;
                width = width/2 + width/4;
                l2.getLayoutParams().height = height;
                l2.getLayoutParams().width = width;
                
                dialog.show();
                
                // 子线程加载数据
                new Thread(new Runnable() {
                    public void run() {
                        getMusicList(name);
                        
                        // 隐藏进度条
                        l5.post(new Runnable() {
                            public void run() {
                                yq.setVisibility(View.GONE);
                            }
                        });
                        
                        // 遍历添加音乐项
                        for (int i = 0; i < song_list.size(); i++) {
                            final int k = i;
                            
                                Thread.sleep(100);
                            l5.post(new Runnable() {
                                public void run() {
                                    try {
                                        // 创建卡片布局
                                        final LinearLayout l3 = new LinearLayout(Thisactixity);
                                        l3.setOrientation(LinearLayout.VERTICAL);
                                        l3.setPadding(15, 15, 15, 15);
                                        l3.setBackground(getShape("#FFFAF4", "#00000000", 10, 20, 180, false));
                                        
                                        LinearLayout.LayoutParams cardParams = new LinearLayout.LayoutParams(
                                            LinearLayout.LayoutParams.MATCH_PARENT,
                                            LinearLayout.LayoutParams.WRAP_CONTENT
                                        );
                                        cardParams.setMargins(0, 0, 0, 8);
                                        l3.setLayoutParams(cardParams);
                                        
                                        // 存储歌曲数据到tag
                                        Object[] songData = new Object[4];
                                        songData[0] = song_list.get(k);
                                        songData[1] = singer_list.get(k);
                                        songData[2] = cover_list.get(k);
                                        songData[3] = k;
                                        l3.setTag(songData);
                                        
                                        // 标题行
                                        LinearLayout titleRow = new LinearLayout(Thisactixity);
                                        titleRow.setOrientation(LinearLayout.HORIZONTAL);
                                        titleRow.setGravity(Gravity.CENTER_VERTICAL);
                                        
                                        // 歌曲信息
                                        LinearLayout infoLayout = new LinearLayout(Thisactixity);
                                        infoLayout.setOrientation(LinearLayout.VERTICAL);
                                        infoLayout.setLayoutParams(new LinearLayout.LayoutParams(
                                            0, LinearLayout.LayoutParams.WRAP_CONTENT, 1
                                        ));
                                        
                                        TextView t1 = new TextView(Thisactixity);
                                        t1.setText(song_list.get(k));
                                        t1.setTextColor(Color.parseColor("#111111"));
                                        t1.setTextSize(18);
                                        t1.setTypeface(null, Typeface.BOLD);
                                        
                                        TextView t2 = new TextView(Thisactixity);
                                        t2.setText("歌手: " + singer_list.get(k));
                                        t2.setTextColor(Color.parseColor("#666666"));
                                        t2.setTextSize(12);
                                        
                                        infoLayout.addView(t1);
                                        infoLayout.addView(t2);
                                        
                                        titleRow.addView(infoLayout);
                                        
                                        // 详情布局（默认隐藏）
                                        final LinearLayout l4 = new LinearLayout(Thisactixity);
                                        l4.setOrientation(LinearLayout.VERTICAL);
                                        l4.setVisibility(View.GONE);
                                        l4.setPadding(0, 15, 0, 0);
                                        
                                        TextView t4 = new TextView(Thisactixity);
                                        t4.setText("点击下方按钮发送音乐");
                                        t4.setTextSize(12);
                                        t4.setTextColor(Color.parseColor("#FF5722"));
                                        t4.setGravity(Gravity.CENTER);
                                        t4.setPadding(0, 10, 0, 15);
                                        
                                        LinearLayout l6 = new LinearLayout(Thisactixity);
                                        l6.setOrientation(LinearLayout.HORIZONTAL);
                                        l6.setGravity(Gravity.CENTER);
                                        
                                        TextView spacer = new TextView(Thisactixity);
                                        spacer.setLayoutParams(new LinearLayout.LayoutParams(
                                            0, LinearLayout.LayoutParams.WRAP_CONTENT, 1
                                        ));
                                        
                                        final Button b1 = new Button(Thisactixity);
                                        b1.setText("发送音乐");
                                        b1.setTextSize(16);
                                        b1.setPadding(30, 10, 30, 10);
                                        b1.setBackground(getShape("#7373B9", "#7373B9", 0, 20, 200, false));
                                        b1.setTextColor(Color.WHITE);
                                        
                                        // 给按钮也设置tag，存储位置
                                        b1.setTag(k);
                                        
                                        TextView spacer2 = new TextView(Thisactixity);
                                        spacer2.setLayoutParams(new LinearLayout.LayoutParams(
                                            0, LinearLayout.LayoutParams.WRAP_CONTENT, 1
                                        ));
                                        
                                        l6.addView(spacer);
                                        l6.addView(b1);
                                        l6.addView(spacer2);
                                        
                                        l4.addView(t4);
                                        l4.addView(l6);
                                        
                                        // 组装
                                        l3.addView(titleRow);
                                        l3.addView(l4);
                                        
                                        // 点击展开/折叠 - 从tag获取数据
                                        l3.setOnClickListener(new View.OnClickListener() {
                                            public void onClick(View v) {
                                                Object[] data = (Object[]) v.getTag();
                                                String songName = (String) data[0];
                                                String singer = (String) data[1];
                                                
                                                if (l4.getVisibility() == View.GONE) {
                                                    t4.setText("发送歌曲: " + songName + " - " + singer);
                                                    expandView(l4);
                                                } else {
                                                    collapseView(l4);
                                                }
                                            }
                                        });
                                        
                                        // 发送按钮点击 - 从tag获取位置
                                        b1.setOnClickListener(new View.OnClickListener() {
                                            public void onClick(View v) {
                                                int position = (Integer) v.getTag();
                                                sendMusic(qun, name, type, msyy, position);
                                                
                                                // 从卡片tag获取歌曲名
                                                View parentCard = (View) v.getParent().getParent().getParent();
                                                Object[] cardData = (Object[]) parentCard.getTag();
                                                String songName = (String) cardData[0];
                                                
                                                Toast("正在发送: " + songName);
                                            }
                                        });
                                        
                                        l5.addView(l3);
                                        
                                    } catch (Exception e) {
                                        Toast("添加项失败: " + e.getMessage());
                                    }
                                }
                            });
                        }
                    }
                }).start();
                
            } catch (Exception e) {
                Toast("创建对话框失败: " + e.getMessage());
            }
        }
    });
}
List song_list = new ArrayList();
List singer_list = new ArrayList();
List mid_list = new ArrayList();
List cover_list = new ArrayList();
public void getMusicList(String msg) {
    song_list.clear();
    singer_list.clear();
    mid_list.clear();
    cover_list.clear();
    try {
        String json = get(myWeb+"qqmusicu.php?msg="+msg+"&num=50&sign="+md5(SECRET+URL(msg,1)));
        JSONObject jsonObject = new JSONObject(json);
        int code=jsonObject.getInt("code");
        if(code == 0) {
            JSONArray dataArray = jsonObject.getJSONArray("data");
            for (int i = 0;
            i < dataArray.length();
            i++) {
                JSONObject item = dataArray.getJSONObject(i);
                String name = item.get("title");
                String singer = item.get("singer");
                String mid = item.get("mid");
                String cover = item.get("cover");
                song_list.add(name);
                //歌曲名字
                singer_list.add(singer);
                //歌手
                mid_list.add(mid);
                //mid
                cover_list.add(cover);
                //cover
            }
            //Toast("歌曲搜索成功！");
        }
        else {
            Toast("获取音乐列表失败:"+jsonObject.getString("msg"));
        }
    }
    catch (e) {
        sendMsg(myUin,"弹窗点歌获取失败"+e,1);
    }
}
public void sendMusic(String qun,String msg,int type,msyy,int k) {
    new Thread(new Runnable() {
        public void run() {
            try {
                String yyms= msyy;
                if(yyms.equals("")) yyms="卡片";
                int i=k+1;
                String url=get(myWeb+"qqmusicu.php?msg="+msg+"&num=30&n="+i+"&sign="+md5(SECRET+URL(msg,1)));
                JSONObject json = new JSONObject(url);
                int code=json.getInt("code");
                if(code==0) {
                    String dataJson=json.getString("data");
                    JSONObject json1 = new JSONObject(dataJson);
                    String cover=json1.optString("cover");
                    String name=json1.optString("title");
                    String singer=json1.optString("singer");
                    String url1=json1.optString("music");
                    String music="https://i.y.qq.com/v8/playsong.html?songmid="+json1.optString("mid");
                    sendMusic(qun,name,singer,music,url1,cover,"QQ",yyms,type);
                }
                else {
                    Toast("获取音乐直链失败请稍后重试");
                }
            }
            catch(e) {
                sendMsg(qun,"弹窗点歌失败，请稍后重试",type);
                sendMsg(myUin,"弹窗点歌失败："+e,1);
            }
        }
    }
    ).start();
}


// 缓存dp转换结果
private static int padding16dp = -1;
private static int padding20dp = -1;

private int getCachedDp(Activity activity, int dp) {
    if(dp == 16 && padding16dp == -1) {
        padding16dp = dpToPx(activity, 16);
        return padding16dp;
    } else if(dp == 20 && padding20dp == -1) {
        padding20dp = dpToPx(activity, 20);
        return padding20dp;
    }
    return dpToPx(activity, dp);
}

// 异步解压文件
public void unzipToModule(Object data){
    new Thread(new Runnable() {
        public void run() {
            try {
                String qun = data.qun;
                String uin = data.uin;
                int mtype = data.mtype;
                int msgType = data.msgtype;
                
                if(msgType == 3) {
                    // 添加空值检查
                    if(data.originMsg == null || data.originMsg.elements == null || 
                       data.originMsg.elements.size() == 0) {
                        Toast("消息数据异常");
                        return;
                    }
                    
                    Object fileElement = data.originMsg.elements.get(0).fileElement;
                    if(fileElement == null) {
                        Toast("文件信息获取失败");
                        return;
                    }
                    
                    String path = fileElement.filePath;
                    File file = new File(path);
                    
                    if(file.exists()) {
                        String filename = file.getName();
                        if(filename.contains(".zip")) {
                            Unzip(path, QQPath);
                            String zipName = getZipName(path);
                            Toast("脚本[" + zipName + "]已解压到" + QQPath);
                        } else {
                            Toast("不是zip文件，无法解压");
                        }
                    } else {
                        Toast("文件不存在，请确认已下载文件");
                    }
                } else {
                    Toast("不是群/好友文件");
                }
            } catch(Exception e) {
                Toast("解压失败: " + e.getMessage());
            }
        }
    }).start();
}

// 优化对话框创建
public void showOptionsDialog(Object data) {
    Activity activity = getNowActivity();
    if (activity == null) return;

    activity.runOnUiThread(new Runnable() {
        public void run() {
            try {
                Dialog dialog = new Dialog(activity);
                dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);

                Window window = dialog.getWindow();
                if (window != null) {
                    window.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
                }

                LinearLayout root = new LinearLayout(activity);
                root.setOrientation(LinearLayout.VERTICAL);
                root.setLayoutParams(new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT));

                GradientDrawable bgDrawable = new GradientDrawable();
                bgDrawable.setColor(Color.WHITE);
                bgDrawable.setCornerRadius(getCachedDp(activity, 16));
                root.setBackground(bgDrawable);

                int padding = getCachedDp(activity, 20);
                root.setPadding(padding, padding, padding, padding);

                // 添加标题
                addDialogTitle(root, activity);
                
                // 添加分割线
                addDivider(root, activity);
                
                // 添加菜单项
                String[] icons = {"📁"};
                String[] texts = {"解压至模块脚本目录"};
                int[] actions = {0};
                
                for (int i = 0; i < icons.length; i++) {
                    addMenuItem(root, activity, icons[i], texts[i], actions[i], dialog, data);
                    
                    if (i < icons.length - 1) {
                        addItemDivider(root, activity);
                    }
                }

                // 添加取消按钮
                addCancelButton(root, activity, dialog);

                dialog.setContentView(root);
                dialog.setCanceledOnTouchOutside(true);
                dialog.show();
            } catch(Exception e) {
            }
        }
    });
}

// 提取的辅助方法
private void addDialogTitle(LinearLayout root, Activity activity) {
    TextView title = new TextView(activity);
    title.setText("冷雨Java · 消息功能");
    title.setTextSize(18);
    title.setTextColor(Color.BLACK);
    title.setTypeface(null, Typeface.BOLD);
    title.setPadding(0, 0, 0, getCachedDp(activity, 12));
    root.addView(title);
}

private void addDivider(LinearLayout root, Activity activity) {
    View divider = new View(activity);
    LinearLayout.LayoutParams dividerLp = new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, 1);
    divider.setLayoutParams(dividerLp);
    divider.setBackgroundColor(Color.parseColor("#DDDDDD"));
    root.addView(divider);
}

private void addMenuItem(LinearLayout root, Activity activity, String icon, String text, 
                         int actionId, Dialog dialog, Object data) {
    TextView item = new TextView(activity);
    item.setText(icon + "  " + text);
    item.setTextSize(16);
    item.setTextColor(Color.BLACK);
    int padding = getCachedDp(activity, 12);
    item.setPadding(padding, padding, padding, padding);
    item.setOnClickListener(new View.OnClickListener() {
        public void onClick(View v) {
            dialog.dismiss();
            if (actionId == 0) {
                unzipToModule(data);
            }
        }
    });
    root.addView(item);
}

private void addItemDivider(LinearLayout root, Activity activity) {
    View itemDivider = new View(activity);
    int margin = getCachedDp(activity, 12);
    LinearLayout.LayoutParams itemDividerLp = new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, 1);
    itemDividerLp.setMargins(margin, 0, margin, 0);
    itemDivider.setLayoutParams(itemDividerLp);
    itemDivider.setBackgroundColor(Color.parseColor("#EEEEEE"));
    root.addView(itemDivider);
}

private void addCancelButton(LinearLayout root, Activity activity, Dialog dialog) {
    Button cancelBtn = new Button(activity);
    cancelBtn.setText("取消");
    cancelBtn.setTextColor(Color.parseColor("#0288D1"));
    cancelBtn.setBackground(null);
    cancelBtn.setAllCaps(false);
    cancelBtn.setGravity(Gravity.CENTER);
    cancelBtn.setPadding(0, getCachedDp(activity, 12), 0, getCachedDp(activity, 8));
    cancelBtn.setOnClickListener(new View.OnClickListener() {
        public void onClick(View v) {
            dialog.dismiss();
        }
    });
    root.addView(cancelBtn);
}

public void menu_call(Object Yu){
    Object data = getData();
    data.put(Yu,""+Module);
    String quntext = data.quntext;
    String qun = data.qun;
    String uin = data.uin;
    String qq=myUin;
    int mtype=data.mtype;
    int msgtype=data.msgtype;
    long msgid=data.msgid;
    
    showOptionsDialog(data);
}
addMenuItem("消息功能", "menu_call");