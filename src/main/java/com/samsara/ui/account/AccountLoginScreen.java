package com.samsara.ui.account;

import com.samsara.ui.terminal.TerminalPage;
import com.samsara.ui.terminal.TerminalTheme;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.util.FormattedCharSequence;

public final class AccountLoginScreen extends Screen implements TerminalPage {
   private final AccountManagerScreen parent;
   private final boolean direct;
   private SavedAccount.Type type;
   private AccountSessions.MicrosoftMethod method = AccountSessions.MicrosoftMethod.WEB_VIEW;
   private boolean onlineId, generate, closed;
   private EditBox input, password;
   private String status = "", code = "", url = "";
   private int fieldsY;
   private Button submit;
   private final List<Button> controls = new ArrayList<>();
   public AccountLoginScreen(AccountManagerScreen parent, boolean direct) {
      super(Component.literal(direct ? "Token 登录" : "添加账号")); this.parent=parent; this.direct=direct;
      type = direct ? SavedAccount.Type.SESSION : SavedAccount.Type.MICROSOFT;
   }
   @Override protected void init() {
      String previous = input == null ? "" : input.getValue(), previousPassword = password == null ? "" : password.getValue();
      controls.clear(); input=null; password=null;
      var bodyLayout=TerminalTheme.body(width,height); int x=bodyLayout.x(), w=bodyLayout.width();
      var available = direct ? List.of(SavedAccount.Type.CRACKED,SavedAccount.Type.SESSION) : List.of(SavedAccount.Type.values());
      int tw=(w-(available.size()-1)*4)/available.size();
      for(int i=0;i<available.size();i++) {
         var tab=available.get(i);
         control((type==tab?"✓ ":"")+tab.label,x+i*(tw+4),bodyLayout.y(),tw,()->{clearSecrets(); type=tab; status=""; rebuildWidgets();});
      }
      fieldsY=bodyLayout.y()+(type==SavedAccount.Type.MICROSOFT?49:29);
      if(type==SavedAccount.Type.MICROSOFT) {
         int mw=(w-8)/3;
         String[] labels={"Web View","设备代码","邮箱密码"};
         for(var authMethod:AccountSessions.MicrosoftMethod.values())
            control((method==authMethod?"✓ ":"")+labels[authMethod.ordinal()],x+authMethod.ordinal()*(mw+4),bodyLayout.y()+26,mw,()->{clearSecrets(); method=authMethod; status=""; rebuildWidgets();});
         if(method==AccountSessions.MicrosoftMethod.CREDENTIALS) {
            input=field("Microsoft 邮箱",x+5,fieldsY+4,w-10,320,false,previous);
            password=field("密码",x+5,fieldsY+28,w-10,1024,true,previousPassword);
         }
      } else {
         String hint=switch(type) {case CRACKED->"Minecraft 用户名";case SESSION->"粘贴 Token 或完整账号资料";default->generate?"TheAltening API Key":"TheAltening Account Token";};
         input=field(hint,x+5,fieldsY+13,w-10,type==SavedAccount.Type.CRACKED?16:type==SavedAccount.Type.SESSION?65536:16384,type!=SavedAccount.Type.CRACKED,previous);
         if(type==SavedAccount.Type.CRACKED) {
            control((onlineId?"✓ ":"")+"使用在线 UUID",x,fieldsY+42,(w-4)/2,()->{onlineId=!onlineId; rebuildWidgets();});
            control("随机用户名",x+(w+4)/2,fieldsY+42,(w-4)/2,()->input.setValue(randomName()));
         } else if(type==SavedAccount.Type.SESSION) {
            control("粘贴 Token",x,fieldsY+42,(w-4)/2,this::pasteToken);
            control("清空",x+(w+4)/2,fieldsY+42,(w-4)/2,()->{input.setValue("");status="";});
         } else if(type==SavedAccount.Type.THEALTENING) {
            control(generate?"✓ API 生成账号":"使用 API 生成账号",x,fieldsY+42,(w-4)/2,()->{clearSecrets(); generate=!generate; rebuildWidgets();});
            control("获取 Account Token",x+(w+4)/2,fieldsY+42,(w-4)/2,()->openUrl("https://thealtening.com"));
         }
      }
      int submitY=Math.min(bodyLayout.footer()-25,fieldsY+(type==SavedAccount.Type.MICROSOFT?51:67));
      submit=control(type==SavedAccount.Type.MICROSOFT&&method==AccountSessions.MicrosoftMethod.WEB_VIEW?"打开 Microsoft 登录窗口":direct?"登录":"添加账号",x,submitY,w,()->start(false));
      if(type==SavedAccount.Type.MICROSOFT&&method==AccountSessions.MicrosoftMethod.DEVICE_CODE) {
         submit.setWidth((w-4)/2); submit.setMessage(Component.literal("浏览器登录"));
         control("复制登录链接",x+(w+4)/2,submitY,(w-4)/2,()->start(true));
      }
      addRenderableWidget(Button.builder(Component.literal("取消 / 返回"),btn->onClose()).bounds(x,bodyLayout.footer(),w,20).build());
      if(!url.isEmpty()) {
         int cw=(w-8)/3;
         submit.visible=false;
         for(var control:controls) if(control.getY()==submitY) control.visible=false;
         independent("打开登录页面",x,submitY,cw,()->openUrl(url));
         independent("复制设备代码",x+cw+4,submitY,cw,()->minecraft.keyboardHandler.setClipboard(code));
         independent("复制登录链接",x+(cw+4)*2,submitY,cw,()->minecraft.keyboardHandler.setClipboard(url));
      }
      if(input!=null) setInitialFocus(input);
      updateEnabled();
   }
   private Button control(String label,int x,int y,int w,Runnable action) {
      Button button=addRenderableWidget(Button.builder(Component.literal(label),btn->{if(!AccountSessions.busy()) action.run();}).bounds(x,y,Math.max(20,w),20).build());
      controls.add(button); return button;
   }
   private void independent(String label,int x,int y,int w,Runnable action) {
      addRenderableWidget(Button.builder(Component.literal(label),btn->action.run()).bounds(x,y,w,20).build());
   }
   private EditBox field(String hint,int x,int y,int w,int max,boolean secret,String previous) {
      boolean tokenField=type==SavedAccount.Type.SESSION;
      var field=new EditBox(font,x,y,w,16,Component.literal(hint)) {
         @Override public void insertText(String value) {
            if(tokenField&&value.length()>1) {
               try {value=AccountAuthClient.sessionToken(value);status="";}
               catch(IllegalArgumentException error) {status=error.getMessage();return;}
            }
            super.insertText(value);
         }
      }; TerminalTheme.input(field);
      field.setHint(Component.literal(hint)); field.setMaxLength(max);
      if(secret) field.addFormatter((text,offset)->FormattedCharSequence.forward("*".repeat(text.length()),Style.EMPTY));
      field.setValue(previous); field.setResponder(value->updateEnabled()); addRenderableWidget(field); return field;
   }
   private void pasteToken() {
      try {input.setValue(AccountAuthClient.sessionToken(minecraft.keyboardHandler.getClipboard()));status="";}
      catch(IllegalArgumentException error) {status=error.getMessage();}
   }
   private void updateEnabled() {
      boolean busy=AccountSessions.busy(); for(var button:controls) button.active=!busy;
      if(input!=null) input.setEditable(!busy); if(password!=null) password.setEditable(!busy);
      if(submit!=null) submit.active=!busy&&(input==null||!input.getValue().isBlank())&&(password==null||!password.getValue().isBlank());
   }
   private void start(boolean copyLink) {
      if(AccountSessions.busy()) return;
      try { if(type==SavedAccount.Type.CRACKED) AccountStore.validate(input.getValue()); }
      catch(IllegalArgumentException error) {status=error.getMessage();return;}
      String value=input==null?"":input.getValue(), secret=password==null?"":password.getValue();
      if(type==SavedAccount.Type.SESSION) {
         try {value=AccountAuthClient.sessionToken(value);}
         catch(IllegalArgumentException error) {status=error.getMessage();return;}
      }
      if(input!=null&&type!=SavedAccount.Type.CRACKED) input.setValue(""); if(password!=null) password.setValue("");
      status="正在验证账号，请稍候"; code=""; url="";
      AccountSessions.authenticate(new AccountSessions.Request(type,value,secret,onlineId,generate,method,null), progress->{
         if(closed) return;
         status=progress.message(); code=progress.code(); url=progress.url();
         if(copyLink) minecraft.keyboardHandler.setClipboard(url); else openUrl(url);
         rebuildWidgets();
      },result->{
         if(closed) return;
         status=result.message(); code=""; url="";
         if(result.success()) {clearSecrets();parent.accept(result,!direct||type==SavedAccount.Type.SESSION,direct);closed=true;minecraft.gui.setScreen(parent);}
         else rebuildWidgets();
      });
      updateEnabled();
   }
   static String randomName() { return "Samsara_"+Integer.toString(ThreadLocalRandom.current().nextInt(36*36*36*36*36),36); }
   private void openUrl(String value) {
      try { com.mojang.blaze3d.Blaze3D.openUri(java.net.URI.create(value)); }
      catch (Exception error) { status="无法打开浏览器，请复制登录链接"; }
   }
   private void clearSecrets() {if(input!=null) input.setValue("");if(password!=null) password.setValue("");}
   @Override public void tick() {updateEnabled();}
   @Override public void extractRenderState(GuiGraphicsExtractor graphics,int mx,int my,float dt) {
      var bodyLayout=TerminalTheme.body(width,height);
      if(input!=null&&type!=SavedAccount.Type.MICROSOFT) graphics.text(font,type==SavedAccount.Type.CRACKED?"用户名":type==SavedAccount.Type.SESSION?"Minecraft 访问令牌":generate?"API Key":"Account Token",bodyLayout.x(),fieldsY,0xFFB1C0CD,false);
      if(type==SavedAccount.Type.MICROSOFT&&method==AccountSessions.MicrosoftMethod.WEB_VIEW)
         graphics.textWithWordWrap(font,Component.literal("在 Microsoft 登录窗口完成授权，支持双重验证\n账号保存后可自动刷新，无需再次输入密码"),bodyLayout.x(),fieldsY,bodyLayout.width(),0xFFB1C0CD,false);
      if(!code.isEmpty()) graphics.text(font,"设备代码："+code,bodyLayout.x(),fieldsY+15,0xFF00D2EB,false);
      String hint=status.isEmpty()?(type==SavedAccount.Type.SESSION?(direct?"登录成功后保存到账号列表；支持粘贴完整账号资料":"支持粘贴完整账号资料；Token 到期后需要更新"):direct?"本次登录不保存账号":"仅保存登录授权，不保存邮箱密码"):status;
      graphics.text(font,font.plainSubstrByWidth(hint,bodyLayout.width()),bodyLayout.x(),height-31,0xFFB8C8D5,false);
      super.extractRenderState(graphics,mx,my,dt);
   }
   @Override public boolean keyPressed(KeyEvent event) {if(event.isConfirmation()&&!AccountSessions.busy()&&submit.active){start(false);return true;}return super.keyPressed(event);}
   @Override public void onClose() {closed=true;AccountSessions.cancel();clearSecrets();minecraft.gui.setScreen(parent);}
   @Override public void extractBackground(GuiGraphicsExtractor graphics,int x,int y,float dt) { }
   @Override public String terminalTitle() {return direct?(type==SavedAccount.Type.SESSION?"Token 登录 / TOKEN LOGIN":"直接登录 / DIRECT LOGIN"):"添加账号 / REGISTER IDENTITY";}
   @Override public String terminalCode() {return "03 / AUTHORIZATION";}
   @Override public String terminalDescription() {return "身份与归属\n每一次旅程，都从你开始";}
   @Override public String terminalStatus() {return AccountSessions.busy()?"AUTHENTICATING / 可取消":"IDENTITY / "+type.label;}
}
