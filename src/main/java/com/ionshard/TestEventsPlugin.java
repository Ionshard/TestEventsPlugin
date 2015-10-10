package com.ionshard;

import com.google.inject.Inject;
import org.slf4j.Logger;
import org.spongepowered.api.Game;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.event.Cancellable;
import org.spongepowered.api.event.EventListener;
import org.spongepowered.api.event.Listener;
import org.spongepowered.api.event.SpongeEventFactory;
import org.spongepowered.api.event.action.LightningEvent;
import org.spongepowered.api.event.block.ChangeBlockEvent;
import org.spongepowered.api.event.cause.Cause;
import org.spongepowered.api.event.game.state.GameStartedServerEvent;
import org.spongepowered.api.event.impl.AbstractEvent;
import org.spongepowered.api.plugin.Plugin;
import org.spongepowered.api.text.Texts;
import org.spongepowered.api.text.chat.ChatTypes;
import org.spongepowered.api.util.command.CommandException;
import org.spongepowered.api.util.command.CommandResult;
import org.spongepowered.api.util.command.CommandSource;
import org.spongepowered.api.util.command.args.CommandContext;
import org.spongepowered.api.util.command.spec.CommandExecutor;
import org.spongepowered.api.util.command.spec.CommandSpec;

import java.util.Optional;

@Plugin(id = "testevents", name = "Test Events Plugin", version = "0.1")
public class TestEventsPlugin {

    @Inject
    private Logger logger;

    private Game game;
    private TestEventsPlugin self;

    //Automatic Registered Plugin Annotation
    @Listener
    public void onPlace(ChangeBlockEvent.Place event) {
        informPlayer(event, "placed");

    }

    //Shows supertype Support
    @Listener
    public void onChange(ChangeBlockEvent event){

        informPlayer(event, "changed");
    }

    //Manually Registered Class Annotation
    private ExampleListener exampleListener;
    public class ExampleListener {

        @Listener
        public void onBreakBlock(ChangeBlockEvent.Break event) {
            informPlayer(event, "broke in annotation");
        }
    }

    //Manually Registered Class Dynamic
    public class ExampleDynamicListener implements EventListener<ChangeBlockEvent.Break> {

        @Override
        public void handle(ChangeBlockEvent.Break event) throws Exception {
            informPlayer(event, "broke in dynamic listener");
        }
    }

    //Receives the manually fired LightningEvent
    @Listener
    public void onLightning(LightningEvent event){
        event.getCause().first(Player.class).get().sendMessage(Texts.of("Ahhh! Lightning!"));
    }

    //Command to Unregister ExampleListener
    CommandSpec unregisterExampleListenerSpec = CommandSpec.builder()
            .description(Texts.of("Unregister ExampleListener"))
            .executor(new CommandExecutor() {
                @Override
                public CommandResult execute(CommandSource src, CommandContext args) throws CommandException {
                    game.getEventManager().unregisterListeners(exampleListener);
                    src.sendMessage(Texts.of("Unregistered ExampleListener"));
                    return CommandResult.success();
                }
            })
            .build();

    //Command to Unregister All Listeners
    CommandSpec unregisterListenersSpec = CommandSpec.builder()
            .description(Texts.of("Unregister All Listeners"))
            .executor(new CommandExecutor() {
                @Override
                public CommandResult execute(CommandSource src, CommandContext args) throws CommandException {
                    game.getEventManager().unregisterPluginListeners(self);
                    src.sendMessage(Texts.of("Unregistered All Listeners"));
                    return CommandResult.success();
                }
            })
            .build();

    //Command to Test SpongeEventFactory
    CommandSpec zeusSpec = CommandSpec.builder()
            .description(Texts.of("Anger Zeus"))
            .executor(new CommandExecutor() {
                @Override
                public CommandResult execute(CommandSource src, CommandContext args) throws CommandException {
                    LightningEvent lightningEvent = SpongeEventFactory.createLightningEvent(game, Cause.of(src));
                    game.getEventManager().post(lightningEvent);
                    src.sendMessage(Texts.of("Zues brings down the Lightning!"));
                    return CommandResult.success();
                }
            })
            .build();

    public class PrivateMessageEvent extends AbstractEvent implements Cancellable {

        private boolean cancelled = false;

        private Player sender;
        private Player recipient;

        private String message;

        public Player getSender() {
            return sender;
        }

        public Player getRecipient() {
            return recipient;
        }

        public String getMessage() {
            return message;
        }

        @Override
        public boolean isCancelled() {
            return cancelled;
        }

        @Override
        public void setCancelled(boolean cancel) {
            cancelled = cancel;
        }

        public PrivateMessageEvent(Player sender, Player recipient, String message) {
            this.sender = sender;
            this.recipient = recipient;
            this.message = message;
        }
    }

    //Command to Test Custom Event
    CommandSpec talkToSelfSpec = CommandSpec.builder()
            .description(Texts.of("Talk to Yourself"))
            .executor(new CommandExecutor() {
                @Override
                public CommandResult execute(CommandSource src, CommandContext args) throws CommandException {
                    Player playerA = (Player) src;
                    Player playerB = (Player) src;
                    PrivateMessageEvent pm = new PrivateMessageEvent(playerA, playerB, "You're crazy");
                    game.getEventManager().post(pm);
                    return CommandResult.success();
                }
            })
            .build();

    @Listener
    public void onPrivateMessage(PrivateMessageEvent event) {
        if(event.getMessage().equals("hi i am from planetminecraft")) {
            event.setCancelled(true);
            return;
        }

        String senderName = event.getSender().getName();
        event.getRecipient().sendMessage(ChatTypes.CHAT, Texts.of("PM from " + senderName + ": " + event.getMessage()));
    }

    //Used to Register Above Listeners
    @Listener
    public void onServerStart(GameStartedServerEvent event) {
        self = this;
        game = event.getGame();

        //Annotated Listeners in Other Class
        exampleListener = new ExampleListener();
        game.getEventManager().registerListeners(this, exampleListener);

        //Dynamic Listener
        EventListener<ChangeBlockEvent.Break> listener = new ExampleDynamicListener();
        game.getEventManager().registerListener(this, ChangeBlockEvent.Break.class, listener);

        //Commands
        game.getCommandDispatcher().register(this, unregisterExampleListenerSpec, "unregisterone");
        game.getCommandDispatcher().register(this, unregisterListenersSpec, "unregisterall");
        game.getCommandDispatcher().register(this, zeusSpec, "zeus");
        game.getCommandDispatcher().register(this, talkToSelfSpec, "talktoself");
    }


    private void informPlayer(ChangeBlockEvent event, String type) {
        final Optional<Player> owner = event.getCause().first(Player.class);
        if (owner.isPresent()) {
            owner.get().sendMessage(Texts.of("Block " + type));
            this.logger.debug(event.getCause().toString());
        }
    }

}

