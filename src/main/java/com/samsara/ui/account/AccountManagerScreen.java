package com.samsara.ui.account;

import com.samsara.ui.terminal.TerminalPage;
import com.samsara.ui.terminal.TerminalTheme;
import com.mojang.authlib.GameProfile;
import com.mojang.blaze3d.platform.InputConstants;
import java.io.IOException;
import java.util.EnumSet;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.Supplier;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.ObjectSelectionList;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.PlayerSkin;

public final class AccountManagerScreen extends Screen implements TerminalPage {
   private final Screen parent;
   private AccountStore store;
   private String status = "选择账号后登录；双击账号可直接登录";
   private final Filters filters = new Filters();
   private UUID selected;
   private EditBox search;
   private AccountsList list;
   private Button login, favorite, delete, up, down, random;
   private boolean wasBusy;
   public AccountManagerScreen(Screen parent) { super(Component.literal("账号设置")); this.parent = parent; }
   @Override protected void init() {
      AccountSessions.captureLauncher();
      loadStore();
      var b = TerminalTheme.body(width, height); int x = b.x(), w = b.width();
      search = new EditBox(font, x + 5, b.y() + 4, Math.max(28, w - 147), 16, Component.literal("搜索用户名或 UUID"));
      search.setHint(Component.literal("搜索用户名 / UUID")); search.setMaxLength(64); TerminalTheme.input(search);
      search.setValue(filters.query); search.setResponder(value -> { filters.query = value; refreshList(); }); addRenderableWidget(search);
      button((filters.premium ? "✓ " : "") + "正版", x + w - 135, b.y(), 64, () -> { filters.premium = !filters.premium; rebuildWidgets(); });
      button((filters.favorites ? "✓ " : "") + "收藏", x + w - 67, b.y(), 67, () -> { filters.favorites = !filters.favorites; rebuildWidgets(); });
      int tw = (w - 9) / 4;
      for (var type : SavedAccount.Type.values()) {
         button((filters.types.contains(type) ? "✓ " : "") + type.label, x + type.ordinal() * (tw + 3), b.y() + 25, tw,
            () -> { if (!filters.types.remove(type)) filters.types.add(type); rebuildWidgets(); });
      }
      list = addRenderableWidget(new AccountsList(w, Math.max(24, b.height() - 80), b.y() + 51, x));
      refreshList();
      int aw = (w - 16) / 5, ay = b.footer() - 28;
      login = button("登录", x, ay, aw, this::loginSelected);
      favorite = button("收藏", x + aw + 4, ay, aw, this::toggleFavorite);
      delete = button("删除", x + (aw + 4) * 2, ay, aw, this::deleteSelected);
      up = button("上移 ↑", x + (aw + 4) * 3, ay, aw, () -> move(-1));
      down = button("下移 ↓", x + (aw + 4) * 4, ay, aw, () -> move(1));
      button("添加", x, b.footer(), aw, () -> { if (store != null) minecraft.gui.setScreen(new AccountLoginScreen(this, false)); });
      button("Token 登录", x + aw + 4, b.footer(), aw, () -> minecraft.gui.setScreen(new AccountLoginScreen(this, true)))
         .setTooltip(Tooltip.create(Component.literal("粘贴 Minecraft Token 或完整账号资料，登录并保存到账号列表")));
      random = button("随机", x + (aw + 4) * 2, b.footer(), aw, this::randomLogin);
      button("恢复", x + (aw + 4) * 3, b.footer(), aw, () -> { AccountSessions.restore(); status = "已恢复启动器账号"; }).setTooltip(Tooltip.create(Component.literal("恢复最初的启动器会话")));
      button("返回", x + (aw + 4) * 4, b.footer(), aw, this::onClose);
      updateActions();
   }
   private Button button(String label, int x, int y, int w, Runnable action) {
      return addRenderableWidget(Button.builder(Component.literal(label), b -> { if (!AccountSessions.busy()) action.run(); }).bounds(x,y,Math.max(20,w),20).build());
   }
   private boolean loadStore() {
      if (store != null) return true;
      try {
         store = new AccountStore(com.samsara.config.ClientDataPaths.accounts(FabricLoader.getInstance().getConfigDir()));
         return true;
      } catch (IOException e) { status = e.getMessage(); return false; }
   }
   private List<SavedAccount> visibleAccounts() { return store == null ? List.of() : filters.apply(store); }
   private SavedAccount selection() { return visibleAccounts().stream().filter(a -> a.key().equals(selected)).findFirst().orElse(null); }
   private void refreshList() {
      if (list == null) return;
      list.replaceEntries(visibleAccounts().stream().map(AccountRow::new).toList());
      for (var row : list.children()) if (row.account.key().equals(selected)) list.setSelected(row);
      updateActions();
   }
   private void updateActions() {
      if (login == null) return;
      var a = selection(); boolean enabled = a != null && !AccountSessions.busy();
      login.active = favorite.active = delete.active = enabled;
      favorite.setMessage(Component.literal(a != null && a.favorite() ? "取消收藏" : "收藏"));
      var visible = visibleAccounts(); int i = a == null ? -1 : visible.indexOf(a);
      up.active = enabled && i > 0; down.active = enabled && i + 1 < visible.size();
      random.active = !AccountSessions.busy() && !visible.isEmpty();
   }
   private void loginSelected() {
      var a = selection(); if (a == null || AccountSessions.busy()) return;
      status = "正在登录 " + a.name();
      AccountSessions.login(a, p -> status = p.message(), result -> {
         status = result.message();
         if (result.success()) {
            try { store.update(result.account()); AccountSessions.activate(result); }
            catch (IOException e) { status = "无法保存刷新后的账号，会话未切换"; }
         }
         refreshList();
      });
   }
   void accept(AccountSessions.Result result, boolean save, boolean activate) {
      status = result.message();
      if (!result.success()) return;
      try {
         if (save) {
            if (!loadStore()) return;
            var account = store.add(result.account());
            selected = account.key(); filters.reveal(account);
            status = (activate ? "已登录并保存 " : "已添加 ") + account.name();
         }
         if (activate) AccountSessions.activate(result);
         refreshList();
      } catch (IOException e) { status = "无法保存账号列表"; }
   }
   private void randomLogin() {
      var accounts = visibleAccounts(); if (accounts.isEmpty()) return;
      selected = accounts.get(ThreadLocalRandom.current().nextInt(accounts.size())).key(); refreshList(); loginSelected();
   }
   private void toggleFavorite() {
      var a = selection(); if (a == null) return;
      try { store.update(a.favorite(!a.favorite())); refreshList(); } catch (IOException e) { status = "无法保存收藏状态"; }
   }
   private void move(int direction) {
      var a = selection(); var visible = visibleAccounts(); int index = visible.indexOf(a);
      if (index < 0 || index + direction < 0 || index + direction >= visible.size()) return;
      reorder(a.key(), visible.get(index + direction).key());
   }
   private void reorder(UUID key, UUID target) {
      try { store.move(key, target); refreshList(); } catch (IOException e) { status = "无法保存账号顺序"; }
   }
   private void deleteSelected() {
      var a = selection(); if (a == null) return;
      minecraft.gui.setScreen(new com.samsara.ui.terminal.TerminalConfirmScreen(this, "删除账号", a.name(), confirmed -> {
         if (confirmed) {
            try { store.remove(a.key()); selected = null; status = "已删除 " + a.name(); refreshList(); }
            catch (IOException e) { status = "无法保存账号列表"; }
         }
      }));
   }
   @Override public void extractRenderState(GuiGraphicsExtractor g, int mx, int my, float dt) {
      var b = TerminalTheme.body(width,height); updateActions(); search.setEditable(!AccountSessions.busy());
      if (visibleAccounts().isEmpty()) g.centeredText(font, store == null ? "账号文件无法加载" : store.accounts().isEmpty() ? "还没有保存账号" : "没有符合筛选条件的账号", b.x() + b.width()/2, b.y()+68, 0xFF9DADBB);
      g.text(font, font.plainSubstrByWidth(status,b.width()), b.x(), height-31, 0xFFB8C8D5, false);
      super.extractRenderState(g,mx,my,dt);
   }
   @Override public void tick() { boolean busy = AccountSessions.busy(); if (wasBusy && !busy) updateActions(); wasBusy = busy; }
   @Override public boolean keyPressed(KeyEvent event) {
      if(event.isConfirmation()&&getFocused()==list&&!AccountSessions.busy()) {loginSelected();return true;}
      return super.keyPressed(event);
   }
   @Override public void onClose() { AccountSessions.cancel(); minecraft.gui.setScreen(parent); }
   @Override public void extractBackground(GuiGraphicsExtractor g, int mx, int my, float dt) { }
   @Override public String terminalTitle() { return "账号设置 / IDENTITY ARCHIVE"; }
   @Override public String terminalCode() { return "03 / ACCOUNTS"; }
   @Override public String terminalDescription() { return "身份与归属\n每一次旅程，都从你开始"; }
   @Override public String terminalStatus() { return "ACTIVE / " + minecraft.getUser().getName() + "    SAVED / " + (store == null ? 0 : store.accounts().size()); }
   static final class Filters {
      String query = "";
      boolean premium, favorites;
      final EnumSet<SavedAccount.Type> types = EnumSet.allOf(SavedAccount.Type.class);
      List<SavedAccount> apply(AccountStore store) { return store.filtered(query, premium, favorites, types); }
      void reveal(SavedAccount account) {
         query = "";
         if (!account.favorite()) favorites = false;
         if (!account.type().premium) premium = false;
         types.add(account.type());
      }
   }
   private final class AccountsList extends ObjectSelectionList<AccountRow> {
      private UUID dragged; private double startY; private boolean moved;
      AccountsList(int w,int h,int y,int x) { super(AccountManagerScreen.this.minecraft,w,h,y,38); setX(x); centerListVertically = false; }
      @Override public int getRowWidth() { return getWidth()-12; }
      @Override public void setSelected(AccountRow row) {
         super.setSelected(row); if(row!=null) selected=row.account.key(); updateActions();
      }
      @Override public boolean mouseClicked(MouseButtonEvent event,boolean doubleClick) {
         boolean result = super.mouseClicked(event,doubleClick);
         var row = getEntryAtPosition(event.x(),event.y());
         if (row != null && event.button() == InputConstants.MOUSE_BUTTON_LEFT && !AccountSessions.busy()) { dragged = row.account.key(); startY = event.y(); moved = false; }
         return result;
      }
      @Override public boolean mouseDragged(MouseButtonEvent event,double dx,double dy) {
         if (dragged != null) { moved |= Math.abs(event.y()-startY)>5; return true; }
         return super.mouseDragged(event,dx,dy);
      }
      @Override public boolean mouseReleased(MouseButtonEvent event) {
         var target = getEntryAtPosition(event.x(),event.y());
         if (dragged != null && moved && target != null && !AccountSessions.busy()) reorder(dragged,target.account.key());
         dragged = null; return super.mouseReleased(event);
      }
   }
   private final class AccountRow extends ObjectSelectionList.Entry<AccountRow> {
      final SavedAccount account; final Supplier<PlayerSkin> skin;
      private long lastClick;
      AccountRow(SavedAccount a) { account = a; skin = minecraft.getSkinManager().createLookup(new GameProfile(a.profileId(),a.name()), false); }
      @Override public Component getNarration() { return Component.literal(account.name()+", "+account.type().label+", "+account.profileId()); }
      @Override public void extractContent(GuiGraphicsExtractor g,int mx,int my,boolean hover,float dt) {
         int x = getContentX(), y = getContentY(); var texture = skin.get().body().texturePath();
         g.blit(RenderPipelines.GUI_TEXTURED, texture,x,y,8,8,24,24,8,8,64,64);
         g.blit(RenderPipelines.GUI_TEXTURED, texture,x,y,40,8,24,24,8,8,64,64);
         int textX=x+30, room=Math.max(20,getContentWidth()-35);
         String label=(account.favorite()?"★ ":"")+account.name()+" / "+account.type().label;
         if (account.profileId().equals(minecraft.getUser().getProfileId())) label+=" / 当前";
         g.text(font,font.plainSubstrByWidth(label,room),textX,y+1,0xFFE2EDF3,false);
         g.text(font,font.plainSubstrByWidth(account.profileId().toString(),room),textX,y+14,0xFF8196A8,false);
         var bans=account.bans().stream().filter(b->b.active(System.currentTimeMillis())).toList();
         if (hover && !bans.isEmpty()) g.setTooltipForNextFrame(font,Component.literal(bans.getFirst().serverName()+" / "+bans.getFirst().reason()),mx,my);
      }
      @Override public boolean mouseClicked(MouseButtonEvent event,boolean doubleClick) {
         if (event.button()!=InputConstants.MOUSE_BUTTON_LEFT || AccountSessions.busy()) return false;
         long now=System.nanoTime(); boolean repeat=account.key().equals(selected)&&lastClick!=0&&now-lastClick<350_000_000L;
         lastClick=now; selected=account.key(); list.setSelected(this); updateActions();
         if (doubleClick||repeat) loginSelected(); return true;
      }
   }
}
