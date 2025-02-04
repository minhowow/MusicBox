package ru.spliterash.musicbox.gui.song;

import com.cryptomorin.xseries.XMaterial;
import com.xxmicloxx.NoteBlockAPI.model.RepeatMode;
import org.bukkit.ChatColor;
import org.bukkit.entity.HumanEntity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import ru.spliterash.musicbox.Lang;
import ru.spliterash.musicbox.customPlayers.abstracts.AbstractBlockPlayer;
import ru.spliterash.musicbox.customPlayers.interfaces.IPlayList;
import ru.spliterash.musicbox.customPlayers.interfaces.MusicBoxSongPlayer;
import ru.spliterash.musicbox.customPlayers.interfaces.PlayerSongPlayer;
import ru.spliterash.musicbox.customPlayers.models.MusicBoxSongPlayerModel;
import ru.spliterash.musicbox.gui.GUIActions;
import ru.spliterash.musicbox.minecraft.gui.GUI;
import ru.spliterash.musicbox.minecraft.gui.InventoryAction;
import ru.spliterash.musicbox.minecraft.gui.actions.ClickAction;
import ru.spliterash.musicbox.minecraft.gui.actions.PlayerClickAction;
import ru.spliterash.musicbox.players.PlayerWrapper;
import ru.spliterash.musicbox.song.MusicBoxSong;
import ru.spliterash.musicbox.song.MusicBoxSongManager;
import ru.spliterash.musicbox.song.songContainers.types.FullSongContainer;
import ru.spliterash.musicbox.utils.BukkitUtils;
import ru.spliterash.musicbox.utils.ItemUtils;
import ru.spliterash.musicbox.utils.SongUtils;
import ru.spliterash.musicbox.utils.StringUtils;
import ru.spliterash.musicbox.utils.classes.PeekList;

import java.util.Collections;
import java.util.List;
import java.util.ListIterator;
import java.util.Set;

/**
 * Контроллер SongPlayer'а
 * Включает в себя
 * - Перемотку
 * - Выбор и просмотр треков в текущем плейлисте
 */
public class SPControlGUI {
    private final MusicBoxSongPlayerModel spModel;
    private final GUI gui;
    private MusicBoxSong currentPlay;
    private boolean closed = false;

    public SPControlGUI(MusicBoxSongPlayerModel songPlayerModel) {
        this.spModel = songPlayerModel;
        this.currentPlay = songPlayerModel.getPlayList().getCurrent();
        this.gui = new GUI(Lang.CONTROL_GUI_TITLE.toString("{song}", currentPlay.getName()), 3);
        refresh();
    }

    public void refresh() {
        IPlayList list = spModel.getPlayList();
        currentPlay = list.getCurrent();
        List<MusicBoxSong> prev = list.getPrevSongs(4);
        Collections.reverse(prev);
        List<MusicBoxSong> next = list.getNextSongs(4);
        PeekList<XMaterial> peekList = new PeekList<>(BukkitUtils.DISCS);
        int startFrom = 4 - prev.size();
        for (ListIterator<MusicBoxSong> iterator = prev.listIterator(); iterator.hasNext(); ) {
            int i = iterator.nextIndex() + startFrom;
            MusicBoxSong s = iterator.next();
            addDiscItem(i, s, peekList, false, list.getSongNum(s));
        }
        addDiscItem(4, currentPlay, peekList, true, list.getSongNum(currentPlay));
        for (ListIterator<MusicBoxSong> iterator = next.listIterator(); iterator.hasNext(); ) {
            int i = iterator.nextIndex();
            MusicBoxSong s = iterator.next();
            addDiscItem(5 + i, s, peekList, false, list.getSongNum(s));
        }
        updateRewind();
        updateControlButtons();
    }

    public void updateControlButtons() {
        MusicBoxSongPlayer player = spModel.getMusicBoxSongPlayer();
        // Кнопка остановки
        {
            gui.addItem(
                    19,
                    ItemUtils.createStack(XMaterial.TORCH, Lang.PARENT_CONTAINER.toString(), null),
                    (spModel.getMusicBoxSongPlayer() instanceof PlayerSongPlayer)
                            ? new ClickAction(() -> MusicBoxSongManager.getRootContainer().createGUI(((PlayerSongPlayer) spModel.getMusicBoxSongPlayer().getApiPlayer()).getModel().getWrapper()).openPage(0, GUIActions.DEFAULT_MODE))
                            : new PlayerClickAction(HumanEntity::closeInventory));
            gui.addItem(
                    20,
                    ItemUtils.createStack(XMaterial.NOTE_BLOCK, Lang.VOLUME_NAME.toString("{value}", Byte.toString(spModel.getMusicBoxSongPlayer().getApiPlayer().getVolume())), Lang.VOLUME_LORE.toList()),
                    new ClickAction(() -> {
                        spModel.getMusicBoxSongPlayer().getApiPlayer().setVolume((byte) (spModel.getMusicBoxSongPlayer().getApiPlayer().getVolume()+5));
                        if (spModel.getMusicBoxSongPlayer() instanceof PlayerSongPlayer) {
                            ((PlayerSongPlayer) spModel.getMusicBoxSongPlayer()).getModel().getWrapper().setVolume(spModel.getMusicBoxSongPlayer().getApiPlayer().getVolume());
                        }
                        updateControlButtons();
                    },
                    () -> {
                        spModel.getMusicBoxSongPlayer().getApiPlayer().setVolume((byte) (spModel.getMusicBoxSongPlayer().getApiPlayer().getVolume()-5));
                        if (spModel.getMusicBoxSongPlayer() instanceof PlayerSongPlayer) {
                            ((PlayerSongPlayer) spModel.getMusicBoxSongPlayer()).getModel().getWrapper().setVolume(spModel.getMusicBoxSongPlayer().getApiPlayer().getVolume());
                        }
                        updateControlButtons();
                    }));
            gui.addItem(22, GUIActions.getStopStack(), new ClickAction(player::destroy));
            ItemStack repeatButton;
            {
                List<String> lore;
                String status = "null";
                if (spModel.getMusicBoxSongPlayer() instanceof AbstractBlockPlayer) {
                    switch(((AbstractBlockPlayer) spModel.getMusicBoxSongPlayer()).getRepeatModeValue()) {
                        case ALL:
                            status = Lang.REPEAT_ALL.toString();
                            break;
                        case NO:
                            status = Lang.REPEAT_NO.toString();
                            break;
                        case ONE:
                            status = Lang.REPEAT_ONE.toString();
                            break;
                    }
                } else {
                    switch(spModel.getMusicBoxSongPlayer().getApiPlayer().getRepeatMode()) {
                        case ALL:
                            status = Lang.REPEAT_ALL.toString();
                            break;
                        case NO:
                            status = Lang.REPEAT_NO.toString();
                            break;
                        case ONE:
                            status = Lang.REPEAT_ONE.toString();
                            break;
                    }
                }
                lore = Lang.SWITH_REPEAT_MODE_LORE.toList("{status}", status);
                repeatButton = ItemUtils.createStack(XMaterial.NOTE_BLOCK, Lang.REPEAT_MODE.toString(), lore);
            }
            gui.addItem(26, repeatButton, new PlayerClickAction(p -> {
                if (p.hasPermission("musicbox.repeat")) {
                    if (spModel.getMusicBoxSongPlayer() instanceof PlayerSongPlayer) {
                        PlayerWrapper.getInstance(p).switchRepeatMode();
                    } else if (spModel.getMusicBoxSongPlayer() instanceof AbstractBlockPlayer){
                        switch(((AbstractBlockPlayer) spModel.getMusicBoxSongPlayer()).getRepeatModeValue()){
                            case NO:
                                ((AbstractBlockPlayer) spModel.getMusicBoxSongPlayer()).setRepeatModeValue(RepeatMode.ALL);
                                break;
                            case ALL:
                                ((AbstractBlockPlayer) spModel.getMusicBoxSongPlayer()).setRepeatModeValue(RepeatMode.ONE);
                                break;
                            case ONE:
                                ((AbstractBlockPlayer) spModel.getMusicBoxSongPlayer()).setRepeatModeValue(RepeatMode.NO);
                                break;
                        }
                    }
                    updateControlButtons();
                } else {
                    p.sendMessage(Lang.CANT_SWITCH_REPEAT.toString());
                }
            }));
        }
    }

    private void addDiscItem(int index, MusicBoxSong song, PeekList<XMaterial> peekList, boolean playNow, int songNum) {
        gui.addItem(index, song.getSongStack(peekList.getAndNext(),
                SongUtils.getSongName(songNum, song, playNow),
                playNow ? Lang.SONG_PANEL_NOW_PLAY.toList() : Lang.SONG_PANEL_SWITH_TO.toList(),
                playNow
        ), new ClickAction(() -> {
            spModel.getPlayList().setSong(song);
            if (spModel.getMusicBoxSongPlayer() instanceof AbstractBlockPlayer) {
                spModel.createNextPlayer();
            } else {
                spModel.playSong(songNum);
            }
        }));
    }

    public void openNext(MusicBoxSongPlayerModel nextModel) {
        Set<Player> set = BukkitUtils.findOpenPlayers(gui);
        if (set.size() > 0) {
            SPControlGUI g = nextModel.getControlGUI();
            set.forEach(g::open);
        }
    }

    private void updateRewind() {
        if (closed) {
            return;
        }
        MusicBoxSongPlayer musicPlayer = spModel.getMusicBoxSongPlayer();
        short allTicks = musicPlayer.getMusicBoxSong().getLength();
        short currentTick = musicPlayer.getTick();
        float speed = musicPlayer.getMusicBoxSong().getSpeed();
        short chunkSize = (short) Math.ceil(allTicks / 9D);
        for (int i = 0; i < 9; i++) {
            int currentIndex = i + 9;
            short chunkStart = (short) (i * chunkSize);
            XMaterial material;
            if (currentTick >= chunkStart) {
                material = XMaterial.WHITE_STAINED_GLASS_PANE;
            } else {
                material = XMaterial.GRAY_STAINED_GLASS_PANE;
            }
            String[] rewindReplaceArray = new String[]{
                    "{percent}", String.valueOf((int) Math.floor(((double) chunkStart / (double) allTicks) * 100)),
                    "{time}", StringUtils.toHumanTime((int) Math.floor(chunkStart / speed))
            };
            gui.addItem(
                    currentIndex,
                    ItemUtils.createStack(
                            material,
                            Lang.REWIND_TO.toString(rewindReplaceArray),
                            null
                    ),
                    new PlayerClickAction(
                            p -> {
                                musicPlayer.getApiPlayer().setTick(chunkStart);
                                p.sendMessage(Lang.REWINDED.toString(rewindReplaceArray));
                                updateRewind();
                            }
                    )
            );
        }
    }

    public void openNoRefresh(Player p) {
        gui.open(p);
    }

    public void open(Player p) {
        openNoRefresh(p);
        refresh();
    }

    public void close() {
        closed = true;
        ItemStack close = ItemUtils.createStack(XMaterial.RED_STAINED_GLASS_PANE, Lang.CLOSE.toString(), null);
        InventoryAction action = new PlayerClickAction(HumanEntity::closeInventory);
        for (int i = 0; i < gui.getInventory().getSize(); i++) {
            gui.addItem(i, close, action);
        }
    }
}
