package org.benefit.mixin;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.client.gui.screen.ingame.LecternScreen;
import net.minecraft.client.gui.screen.ingame.SignEditScreen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.network.packet.Packet;
import net.minecraft.network.packet.c2s.play.CloseHandledScreenC2SPacket;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.text.Text;
import org.benefit.Client;
import org.benefit.Variables;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;


@Mixin(HandledScreen.class)
public abstract class HandledScreenMixin extends Screen {
    @Shadow
    public abstract boolean keyPressed(int keyCode, int scanCode, int modifiers);
    protected HandledScreenMixin(Text title) {
        super(title);
    }
//define variables
    private static final MinecraftClient mc = MinecraftClient.getInstance();
    private static final ClientPlayerEntity mcp = mc.player;

//the main method
    @Inject(at = @At("TAIL"), method = "init")
    public void init(CallbackInfo ci) {

    //simplify expressions
        Boolean SendUIPackets = Variables.sendUIPackets;
        ScreenHandler CurrentScreenHandler = mcp.currentScreenHandler;
        Screen CurrentScreen = mc.currentScreen;
        ClientPlayNetworkHandler mcNetworkHandler = mc.getNetworkHandler();

    //define variables
        int SyncID = mcp.currentScreenHandler.syncId;
        CloseHandledScreenC2SPacket CloseHandledScreen = new CloseHandledScreenC2SPacket(SyncID);

    //define color codes
        String Bold = "§l";
        String LightGray = "§7";
        String Green = "§a";

    //combine bold and lightgray
        String BoldGray = Bold + LightGray;

    //combine bold and green
        String BoldGreen = Bold + Green;

    //define button names
        Text SoftClose = Text.of("Soft Close");
        Text DeSync = Text.of("De-sync");
        Text SaveUI = Text.of("Save UI");
        Text LeaveNSend = Text.of("Leave & send packets");


    //add in send packets button
        addDrawableChild(ButtonWidget.builder(Text.of("Send Packets: " + SendUIPackets), button -> {
                    SendUIPackets.equals(!SendUIPackets);

               //setting the text on the button to true or false when it is active
                    button.setMessage(Text.of("Send Packets: " + Variables.sendUIPackets));

                }).dimensions(835, 65, 120, 20).build());

    //add in delay packets button
        addDrawableChild(ButtonWidget.builder(Text.of("Delay packets: " + Variables.delayUIPackets), (button) -> {
            Variables.delayUIPackets = !Variables.delayUIPackets;

        //setting the text on the button to true or false when it is active
            button.setMessage(Text.of("Delay packets: " + Variables.delayUIPackets));

        //condition to see if any delayed packets was delayed, then send them
            if (!Variables.delayUIPackets && !Variables.delayedPackets.isEmpty()) {
                for (Packet<?> packet : Variables.delayedPackets) {
                    mc.getNetworkHandler().sendPacket(packet);
                }

            //add in message to say how many delayed packets were sent
                int DelayedPacketsCount = Variables.delayedPackets.size();
                mc.player.sendMessage(Text.of(BoldGray + "Successfully sent " + BoldGreen + DelayedPacketsCount + LightGray + " delayed packets."));
                Variables.delayedPackets.clear();
            }
        }).width(120).position(835, 95).build());

    //add in softclose button
        addDrawableChild(ButtonWidget.builder(SoftClose, (button) -> mc.setScreen(null)).width(80).position(875, 5).build());

    //add in desync button
        addDrawableChild(ButtonWidget.builder(DeSync, (button) -> mcNetworkHandler.sendPacket(CloseHandledScreen)).width(80).position(875, 35).build());

    //add in save ui button
        addDrawableChild(ButtonWidget.builder(SaveUI, (button) -> {

        //define variables
            Variables.storedScreen = CurrentScreen;
            Variables.storedScreenHandler = CurrentScreenHandler;

        }).width(80).position(875, 125).build());

    //add in leave n send packets button
        addDrawableChild(ButtonWidget.builder(LeaveNSend, (button) -> {

            if (!Variables.delayedPackets.isEmpty()) {
                Variables.delayUIPackets = false;

                for (Packet<?> packet : Variables.delayedPackets) {
                    mc.getNetworkHandler().sendPacket(packet);
                }
            //add in message to say how many delayed packets were sent
                int DelayedPacketsAmount = Variables.delayedPackets.size();
                
            //disconnect player
                mc.getNetworkHandler().getConnection().disconnect(Text.of(BoldGray + "Disconnected, " + BoldGreen + DelayedPacketsAmount + BoldGray + " packets successfully sent."));
                Variables.delayedPackets.clear();
            }
        }).width(140).position(815, 155).build());
        if ((Object) this instanceof SignEditScreen) {
            //set the screen to null with softclose button
            addDrawableChild(ButtonWidget.builder(Text.of("Soft Close"), (button) -> MinecraftClient.getInstance().setScreen(null)).width(80).position(875, 5).build());
        }
    }


//make sinkid and revision visible
    @Inject(at = @At("TAIL"), method = "render")
    public void render(MatrixStack matrices, int mouseX, int mouseY, float delta, CallbackInfo ci) {
        Client.renderHandledScreen(mc, textRenderer, matrices);
    }
}