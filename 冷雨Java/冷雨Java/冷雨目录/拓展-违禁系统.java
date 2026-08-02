public void 违禁系统(Object Yu) {
    new Thread(new Runnable() {
        public void run() {
            Object data = getData();
            data.put(Yu,Module);
            String quntext = data.quntext;
            String qun = data.qun;
            String uin = data.uin;
            String qq=myUin;
            int mtype=data.mtype;
            long msgid=data.msgid;
            int msgtype=data.msgtype;
            if(mtype==2) {
                if(读(qun,"lengyu520","开关")==1&&cdxz(qun,uin)&&判断群(qun,mtype)==1) {
                    if(quntext.equals("开启违禁系统")) {
                        if(qq.equals(uin)||读(qun,"代管",uin)==1||读("0","代管",uin)==1) {
                            写(qun,"违禁系统","开关",1);
                            String menu="已开启本聊天违禁系统";
                            sendText(data,menu);
                        }
                    }
                    if(quntext.equals("关闭违禁系统")) {
                        if(qq.equals(uin)||读(qun,"代管",uin)==1||读("0","代管",uin)==1) {
                            写(qun,"违禁系统","开关",0);
                            String menu="已关闭本聊天违禁系统";
                            sendText(data,menu);
                        }
                    }
                    if(quntext.equals("违禁系统")) {
                        if(读(qun,"违禁系统","开关")==1) {
                            String menu = "违禁系统:\n开启/关闭违禁系统\n添加违禁词+内容\n删除违禁词+内容\n设置违禁禁言+时间(分)\n查看违禁词+内容\n违禁词列表\n清空违禁词";
                            sendText(data,menu);
                        }
                        else {
                            sendText(data,"本聊天未开启违禁系统");
                        }
                    }
                    if(读(qun,"违禁系统","开关")==1) {
                        if(!uin.equals(qq) && !uin.equals("2854196310"))
                        {
                            if(msgtype==2&&data.originMsg.elements.get(0).picElement!=null) quntext=data.originMsg.elements.get(0).picElement.summary;
                            if(quntext.equals("")) quntext = data.originMsg.toString();
                            cb = quntext.replace("⁡", "");
                            cd = cb.replace("'", "");
                            cf = cb.replace(" ", "");
                            cg = cf.replace(".", "");
                            ch = cg.replace("•", "");
                            op = ch.replace(",", "");
                            int a = 读(qun, "白名单",uin);
                            int b = 读("0", "白名单",uin);
                            int c = 读(qun, "代管",uin);
                            int d = 读("0", "代管",uin);
                            if(uin.equals(qq) || a==1 ||b==1||c==1||d==1)
                            {
                            }
                            else
                            {
                                String[] 违禁词列表 = 列表2(qun, "违禁词列表");
                                boolean tf=false;
                                for(String u: 违禁词列表)
                                {
                                    if(op.contains(u)&&!op.equals(""))
                                    {
                                        tf=true;
                                        break;
                                    }
                                }
                                if(tf) {
                                    sendText(data, "QQ" + uin + "\n触发违禁词");
                                    recallMsg(qun,data.msgid,2);
                                    if(读(qun, "撤回踢出", "开关") == 1)
                                    {
                                        kickGroup(qun, uin, false);
                                    }
                                    int jy = 读(qun, "违禁词系统", "禁言时间");
                                    shutUp(qun, uin, jy * 60);
                                }
                            }
                        }
                        if(quntext.matches("设置违禁禁言[0-9]+"))
                        {
                            if(qq.equals(uin) || 读(qun, "代管", uin) == 1 || 读("0", "代管", uin) == 1)
                            {
                                int one = Long.parseLong(quntext.substring(6));
                                写(qun, "违禁词系统", "禁言时间", one);
                                sendText(data, "写入违禁时间成功～");
                            }
                        }
                        if(quntext.startsWith("添加违禁词"))
                        {
                            String one = quntext.substring(5);
                            if(one.equals(""))
                            {
                                if(读(qun, "代管", uin) == 1 || 读("0", "代管", uin) == 1){
                                    sendText(data, "输入为空");
                                }else if(qq.equals(uin)){
                                    addBanedWords(qun);
                                }
                            }
                            else if(qq.equals(uin) || 读(qun, "代管", uin) == 1 || 读("0", "代管", uin) == 1)
                            {
                                if(读(qun, "违禁词列表", one) == 1)
                                {
                                    sendText(data, "已添加过该违禁词～");
                                }
                                else
                                {
                                    写(qun, "违禁词列表", one, 1);
                                    sendText(data, "写入违禁词成功～");
                                }
                            }
                        }
                        if(quntext.startsWith("查看违禁词"))
                        {
                            if(qq.equals(uin) || 读(qun, "代管", uin) == 1 || 读("0", "代管", uin) == 1)
                            {
                                String one = quntext.substring(5);
                                if(读(qun, "违禁词列表", one) == 1)
                                {
                                    sendText(data, "已有该违禁词～");
                                }
                            }
                        }
                        if(quntext.startsWith("删除违禁词"))
                        {
                            if(qq.equals(uin) || 读(qun, "代管", uin) == 1 || 读("0", "代管", uin) == 1)
                            {
                                String text = quntext.substring(5);
                                if(读(qun, "违禁词列表", text) != 1)
                                {
                                    sendText(data, "该违禁词不在本群违禁词列表中哦～");
                                }
                                else
                                {
                                    清除(qun, "违禁词列表", text);
                                    sendText(data, "已删除该违禁词～");
                                }
                            }
                        }
                        if(quntext.equals("违禁词列表"))
                        {
                            if(qq.equals(uin))
                            {
                            showBannedWordsList(qun);
                            }
                            else if(读("0", "代管", uin) == 1 || 读("" + qun, "代管", uin) == 1)
                            {
                                String[] List = 列表2(qun, "违禁词列表");
                                String x = "本群违禁词列表有:";
                                String a = x;
                                long i = 0;
                                for(String s: List)
                                {
                                    i++;
                                    x = x + "\n" + i + "." + s;
                                }
                                if(x.equals(a)) x = "目前还没有违禁词哦～";
                                put(ColdRainPath + "本群违禁词列表.txt", "" + x);
                                sendFile(qun, ColdRainPath + "本群违禁词列表.txt", 2);
                                sendText(data, "违禁词列表已发送，请查看");
                                //sc(ColdRainPath + "本群违禁词列表.txt");
                            }
                        }
                        if(quntext.equals("清空违禁词"))
                        {
                            if(qq.equals(uin) || 读(qun, "代管", uin) == 1 || 读("0", "代管", uin) == 1)
                            {
                                删除(qun, "违禁词列表");
                                sendText(data, "已清空本群违禁词列表～");
                            }
                        }
                    }
                }
            }
        }
    }
    ).start();
}

public void addBanedWords(String qun) {
    Activity ThisActivity = getNowActivity();
    ThisActivity.runOnUiThread(new Runnable() {
        public void run() {
            
            LinearLayout mainLayout = new LinearLayout(ThisActivity);
            mainLayout.setOrientation(LinearLayout.VERTICAL);
            mainLayout.setPadding(40, 30, 40, 30);
            mainLayout.setBackground(getShape("#FFFFFF", "#E8F5E8", 2, 25, 255, true));
            
            
            TextView title = new TextView(ThisActivity);
            title.setText("添加违禁词");
            title.setTextColor(Color.parseColor("#2E7D32"));
            title.setTextSize(20);
            title.setTypeface(null, Typeface.BOLD);
            title.setGravity(Gravity.CENTER);
            LinearLayout.LayoutParams titleParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, 
                LinearLayout.LayoutParams.WRAP_CONTENT
            );
            titleParams.setMargins(0, 0, 0, 20);
            title.setLayoutParams(titleParams);
            
            
            TextView inputHint = new TextView(ThisActivity);
            inputHint.setText("请输入要添加的违禁词");
            inputHint.setTextColor(Color.parseColor("#424242"));
            inputHint.setTextSize(14);
            inputHint.setLayoutParams(new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, 
                LinearLayout.LayoutParams.WRAP_CONTENT
            ));
            
            
            final EditText editText = new EditText(ThisActivity);
            editText.setHint("输入违禁词内容...");
            editText.setText("");
            editText.setBackground(getShape("#FFFFFF", "#C8E6C9", 2, 15, 255, false));
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
            
            
            TextView description = new TextView(ThisActivity);
            description.setText("💡 提示：添加后，当群成员发送包含该词的消息时，机器人会自动处理");
            description.setTextColor(Color.parseColor("#666666"));
            description.setTextSize(12);
            description.setPadding(0, 0, 0, 20);
            description.setLayoutParams(new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, 
                LinearLayout.LayoutParams.WRAP_CONTENT
            ));
            
            
            int bannedWordsCount = getBannedWordsCount(qun);
            TextView countText = new TextView(ThisActivity);
            countText.setText("当前群聊已有 " + bannedWordsCount + " 个违禁词");
            countText.setTextColor(Color.parseColor("#FF9800"));
            countText.setTextSize(12);
            countText.setTypeface(null, Typeface.BOLD);
            countText.setGravity(Gravity.CENTER);
            countText.setPadding(0, 0, 0, 20);
            countText.setLayoutParams(new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, 
                LinearLayout.LayoutParams.WRAP_CONTENT
            ));
            
            
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
            confirmBtn.setText("添加");
            confirmBtn.setTextColor(Color.parseColor("#FFFFFF"));
            confirmBtn.setBackground(getShape("#4CAF50", "#388E3C", 0, 20, 255, false));
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
            mainLayout.addView(description);
            mainLayout.addView(countText);
            mainLayout.addView(buttonLayout);
            
            
            final Dialog dialog = new Dialog(ThisActivity);
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
            
            
            cancelBtn.setOnClickListener(new View.OnClickListener() {
                public void onClick(View v) {
                    dialog.dismiss();
                }
            });
            
            confirmBtn.setOnClickListener(new View.OnClickListener() {
                public void onClick(View v) {
                    String word = editText.getText().toString().trim();
                    if (word.equals("")) {
                        Toast.makeText(ThisActivity, "请输入违禁词内容", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    
                    if (word.length() > 50) {
                        Toast.makeText(ThisActivity, "违禁词过长，请控制在50字以内", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    
                    
                    if(读(qun, "违禁词列表", word) == 1) {
                        Toast.makeText(ThisActivity, "已添加过该违禁词～", Toast.LENGTH_SHORT).show();
                    } else {
                        
                        写(qun, "违禁词列表", word, 1);
                        Toast.makeText(ThisActivity, "添加违禁词成功～", Toast.LENGTH_SHORT).show();
                        dialog.dismiss();
                        
                        
                        
                    }
                }
            });
            
            
            editText.requestFocus();
            dialog.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE);
            
            dialog.show();
        }
    });
}


public int getBannedWordsCount(String qun) {
    try {
        String[] bannedWordsList = 列表2(qun, "违禁词列表");
        if (bannedWordsList != null) {
            return bannedWordsList.length;
        }
    } catch (Exception e) {
        e.printStackTrace();
    }
    return 0;
}

public void showBannedWordsList(String qun) {
    Activity ThisActivity = getNowActivity();
    ThisActivity.runOnUiThread(new Runnable() {
        public void run() {
            
            final String[] bannedWordsList = 列表2(qun, "违禁词列表");
            final int wordCount = bannedWordsList != null ? bannedWordsList.length : 0;
            
            
            LinearLayout mainLayout = new LinearLayout(ThisActivity);
            mainLayout.setOrientation(LinearLayout.VERTICAL);
            mainLayout.setPadding(30, 25, 30, 20);
            mainLayout.setBackground(getShape("#FFFFFF", "#FFF3E0", 2, 25, 255, true));
            
            
            TextView title = new TextView(ThisActivity);
            title.setText("违禁词管理");
            title.setTextColor(Color.parseColor("#E65100"));
            title.setTextSize(20);
            title.setTypeface(null, Typeface.BOLD);
            title.setGravity(Gravity.CENTER);
            LinearLayout.LayoutParams titleParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, 
                LinearLayout.LayoutParams.WRAP_CONTENT
            );
            titleParams.setMargins(0, 0, 0, 15);
            title.setLayoutParams(titleParams);
            
            
            TextView statsText = new TextView(ThisActivity);
            statsText.setText("当前群聊共有 " + wordCount + " 个违禁词");
            statsText.setTextColor(Color.parseColor("#FF9800"));
            statsText.setTextSize(14);
            statsText.setTypeface(null, Typeface.BOLD);
            statsText.setGravity(Gravity.CENTER);
            statsText.setPadding(0, 0, 0, 15);
            statsText.setLayoutParams(new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, 
                LinearLayout.LayoutParams.WRAP_CONTENT
            ));
            
            
            LinearLayout listContainer = new LinearLayout(ThisActivity);
            listContainer.setOrientation(LinearLayout.VERTICAL);
            listContainer.setPadding(10, 10, 10, 10);
            listContainer.setBackground(getShape("#FFF8E1", "#FFE0B2", 1, 15, 255, false));
            LinearLayout.LayoutParams containerParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, 
                LinearLayout.LayoutParams.WRAP_CONTENT
            );
            containerParams.setMargins(0, 0, 0, 20);
            containerParams.height = dpToPx(ThisActivity, 250); 
            listContainer.setLayoutParams(containerParams);
            
            
            ScrollView scrollView = new ScrollView(ThisActivity);
            LinearLayout.LayoutParams scrollParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, 
                LinearLayout.LayoutParams.MATCH_PARENT
            );
            scrollView.setLayoutParams(scrollParams);
            
            
            LinearLayout wordsLayout = new LinearLayout(ThisActivity);
            wordsLayout.setOrientation(LinearLayout.VERTICAL);
            wordsLayout.setPadding(10, 10, 10, 10);
            
            if (wordCount == 0) {
                TextView emptyText = new TextView(ThisActivity);
                emptyText.setText("目前还没有违禁词哦～");
                emptyText.setTextColor(Color.parseColor("#9E9E9E"));
                emptyText.setTextSize(14);
                emptyText.setGravity(Gravity.CENTER);
                emptyText.setPadding(0, 20, 0, 20);
                wordsLayout.addView(emptyText);
            } else {
                for (int i = 0; i < bannedWordsList.length; i++) {
                    final int index = i;
                    final String word = bannedWordsList[i];
                    
                    
                    LinearLayout wordItem = new LinearLayout(ThisActivity);
                    wordItem.setOrientation(LinearLayout.HORIZONTAL);
                    wordItem.setPadding(15, 12, 15, 12);
                    wordItem.setBackground(getShape("#FFFFFF", "#FFCCBC", 1, 10, 255, false));
                    wordItem.setGravity(Gravity.CENTER_VERTICAL);
                    LinearLayout.LayoutParams itemParams = new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT, 
                        LinearLayout.LayoutParams.WRAP_CONTENT
                    );
                    if (i > 0) {
                        itemParams.setMargins(0, 5, 0, 0);
                    }
                    wordItem.setLayoutParams(itemParams);
                    
                    
                    TextView numberText = new TextView(ThisActivity);
                    numberText.setText((i + 1) + ".");
                    numberText.setTextColor(Color.parseColor("#E65100"));
                    numberText.setTextSize(14);
                    numberText.setTypeface(null, Typeface.BOLD);
                    numberText.setLayoutParams(new LinearLayout.LayoutParams(
                        dpToPx(ThisActivity, 30), 
                        LinearLayout.LayoutParams.WRAP_CONTENT
                    ));
                    
                    
                    TextView wordText = new TextView(ThisActivity);
                    wordText.setText(word);
                    wordText.setTextColor(Color.parseColor("#333333"));
                    wordText.setTextSize(14);
                    wordText.setLayoutParams(new LinearLayout.LayoutParams(
                        0, 
                        LinearLayout.LayoutParams.WRAP_CONTENT, 
                        1.0f
                    ));
                    wordText.setPadding(10, 0, 10, 0);
                    
                    
                    Button deleteBtn = new Button(ThisActivity);
                    deleteBtn.setText("删除");
                    deleteBtn.setTextColor(Color.parseColor("#FFFFFF"));
                    deleteBtn.setTextSize(12);
                    deleteBtn.setBackground(getShape("#FF5722", "#D84315", 0, 8, 255, false));
                    deleteBtn.setPadding(15, 8, 15, 8);
                    deleteBtn.setLayoutParams(new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.WRAP_CONTENT, 
                        LinearLayout.LayoutParams.WRAP_CONTENT
                    ));
                    

deleteBtn.setOnClickListener(new View.OnClickListener() {
    public void onClick(View v) {
        
        final Dialog confirmDialog = new Dialog(ThisActivity);
        confirmDialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
        
        LinearLayout dialogLayout = new LinearLayout(ThisActivity);
        dialogLayout.setOrientation(LinearLayout.VERTICAL);
        dialogLayout.setPadding(40, 30, 40, 25);
        dialogLayout.setBackground(getShape("#FFFFFF", "#FFEBEE", 2, 20, 255, true));
        
        
        TextView warningIcon = new TextView(ThisActivity);
        warningIcon.setText("⚠️");
        warningIcon.setTextSize(30);
        warningIcon.setGravity(Gravity.CENTER);
        LinearLayout.LayoutParams iconParams = new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, 
            LinearLayout.LayoutParams.WRAP_CONTENT
        );
        iconParams.setMargins(0, 0, 0, 15);
        warningIcon.setLayoutParams(iconParams);
        
        
        TextView dialogTitle = new TextView(ThisActivity);
        dialogTitle.setText("确认删除");
        dialogTitle.setTextColor(Color.parseColor("#D32F2F"));
        dialogTitle.setTextSize(18);
        dialogTitle.setTypeface(null, Typeface.BOLD);
        dialogTitle.setGravity(Gravity.CENTER);
        LinearLayout.LayoutParams titleParams = new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, 
            LinearLayout.LayoutParams.WRAP_CONTENT
        );
        titleParams.setMargins(0, 0, 0, 10);
        dialogTitle.setLayoutParams(titleParams);
        
        
        TextView messageText = new TextView(ThisActivity);
        messageText.setText("确定要删除违禁词：\n\"" + word + "\" 吗？");
        messageText.setTextColor(Color.parseColor("#666666"));
        messageText.setTextSize(14);
        messageText.setGravity(Gravity.CENTER);
        messageText.setLineSpacing(1.2f, 1.2f);
        LinearLayout.LayoutParams msgParams = new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, 
            LinearLayout.LayoutParams.WRAP_CONTENT
        );
        msgParams.setMargins(0, 0, 0, 25);
        messageText.setLayoutParams(msgParams);
        
        
        LinearLayout buttonLayout = new LinearLayout(ThisActivity);
        buttonLayout.setOrientation(LinearLayout.HORIZONTAL);
        buttonLayout.setGravity(Gravity.CENTER_HORIZONTAL);
        LinearLayout.LayoutParams btnLayoutParams = new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, 
            LinearLayout.LayoutParams.WRAP_CONTENT
        );
        buttonLayout.setLayoutParams(btnLayoutParams);
        
        
        Button cancelBtn = new Button(ThisActivity);
        cancelBtn.setText("取消");
        cancelBtn.setTextColor(Color.parseColor("#666666"));
        cancelBtn.setBackground(getShape("#F5F5F5", "#E0E0E0", 1, 15, 255, false));
        cancelBtn.setPadding(25, 12, 25, 12);
        cancelBtn.setTextSize(14);
        LinearLayout.LayoutParams cancelParams = new LinearLayout.LayoutParams(
            0, 
            LinearLayout.LayoutParams.WRAP_CONTENT, 
            1.0f
        );
        cancelParams.setMargins(0, 0, 8, 0);
        cancelBtn.setLayoutParams(cancelParams);
        
        
        Button confirmBtn = new Button(ThisActivity);
        confirmBtn.setText("确定删除");
        confirmBtn.setTextColor(Color.parseColor("#FFFFFF"));
        confirmBtn.setBackground(getShape("#D32F2F", "#B71C1C", 0, 15, 255, false));
        confirmBtn.setPadding(25, 12, 25, 12);
        confirmBtn.setTextSize(14);
        LinearLayout.LayoutParams confirmParams = new LinearLayout.LayoutParams(
            0, 
            LinearLayout.LayoutParams.WRAP_CONTENT, 
            1.0f
        );
        confirmParams.setMargins(8, 0, 0, 0);
        confirmBtn.setLayoutParams(confirmParams);
        
        buttonLayout.addView(cancelBtn);
        buttonLayout.addView(confirmBtn);
        
        dialogLayout.addView(warningIcon);
        dialogLayout.addView(dialogTitle);
        dialogLayout.addView(messageText);
        dialogLayout.addView(buttonLayout);
        
        confirmDialog.setContentView(dialogLayout);
        confirmDialog.setCancelable(true);
        
        
        WindowManager wm = (WindowManager) ThisActivity.getSystemService(Context.WINDOW_SERVICE);
        int width = (int) (wm.getDefaultDisplay().getWidth() * 0.75);
        WindowManager.LayoutParams lp = new WindowManager.LayoutParams();
        lp.copyFrom(confirmDialog.getWindow().getAttributes());
        lp.width = width;
        lp.height = WindowManager.LayoutParams.WRAP_CONTENT;
        confirmDialog.getWindow().setAttributes(lp);
        
        
        cancelBtn.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                confirmDialog.dismiss();
            }
        });
        
        confirmBtn.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                
                清除(qun, "违禁词列表", word);
                Toast.makeText(ThisActivity, "✅ 已删除违禁词: " + word, Toast.LENGTH_SHORT).show();
                confirmDialog.dismiss();
                dialog.dismiss();
                
                showBannedWordsList(qun);
            }
        });
        
        confirmDialog.show();
    }
});
                    
                    wordItem.addView(numberText);
                    wordItem.addView(wordText);
                    wordItem.addView(deleteBtn);
                    wordsLayout.addView(wordItem);
                }
            }
            
            scrollView.addView(wordsLayout);
            listContainer.addView(scrollView);
            
            
            LinearLayout buttonLayout = new LinearLayout(ThisActivity);
            buttonLayout.setOrientation(LinearLayout.HORIZONTAL);
            buttonLayout.setGravity(Gravity.CENTER_HORIZONTAL);
            LinearLayout.LayoutParams buttonLayoutParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, 
                LinearLayout.LayoutParams.WRAP_CONTENT
            );
            buttonLayout.setLayoutParams(buttonLayoutParams);
            
            
            Button addBtn = new Button(ThisActivity);
            addBtn.setText("添加违禁词");
            addBtn.setTextColor(Color.parseColor("#FFFFFF"));
            addBtn.setBackground(getShape("#4CAF50", "#388E3C", 0, 20, 255, false));
            addBtn.setPadding(30, 12, 30, 12);
            addBtn.setTextSize(14);
            LinearLayout.LayoutParams addParams = new LinearLayout.LayoutParams(
                0, 
                LinearLayout.LayoutParams.WRAP_CONTENT, 
                1.0f
            );
            addParams.setMargins(0, 0, 5, 0);
            addBtn.setLayoutParams(addParams);
            
            
            Button closeBtn = new Button(ThisActivity);
            closeBtn.setText("关闭");
            closeBtn.setTextColor(Color.parseColor("#666666"));
            closeBtn.setBackground(getShape("#F5F5F5", "#E0E0E0", 1, 20, 255, false));
            closeBtn.setPadding(30, 12, 30, 12);
            closeBtn.setTextSize(14);
            LinearLayout.LayoutParams closeParams = new LinearLayout.LayoutParams(
                0, 
                LinearLayout.LayoutParams.WRAP_CONTENT, 
                1.0f
            );
            closeParams.setMargins(5, 0, 0, 0);
            closeBtn.setLayoutParams(closeParams);
            
            buttonLayout.addView(addBtn);
            buttonLayout.addView(closeBtn);
            
            mainLayout.addView(title);
            mainLayout.addView(statsText);
            mainLayout.addView(listContainer);
            mainLayout.addView(buttonLayout);
            
            
            final Dialog dialog = new Dialog(ThisActivity);
            dialog.setContentView(mainLayout);
            dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
            dialog.setCancelable(true);
            
            
            WindowManager wm = (WindowManager) ThisActivity.getSystemService(Context.WINDOW_SERVICE);
            Display display = wm.getDefaultDisplay();
            int width = (int) (display.getWidth() * 0.90);
            int height = (int) (display.getHeight() * 0.50);
            WindowManager.LayoutParams layoutParams = new WindowManager.LayoutParams();
            layoutParams.copyFrom(dialog.getWindow().getAttributes());
            layoutParams.width = width;
            layoutParams.height = height;
            dialog.getWindow().setAttributes(layoutParams);
            
            
            addBtn.setOnClickListener(new View.OnClickListener() {
                public void onClick(View v) {
                    dialog.dismiss();
                    addBanedWords(qun);
                }
            });
            
            closeBtn.setOnClickListener(new View.OnClickListener() {
                public void onClick(View v) {
                    dialog.dismiss();
                }
            });
            
            dialog.show();
        }
    });
}