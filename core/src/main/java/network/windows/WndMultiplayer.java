package network.windows;

// import com.shatteredpixel.shatteredpixeldungeon.Chrome;
import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.SPDSettings;
import com.shatteredpixel.shatteredpixeldungeon.ShatteredPixelDungeon;
import com.shatteredpixel.shatteredpixeldungeon.messages.Messages;

import network.MPSettings;
import network.Multiplayer;
import network.NetworkManager;
// import com.shatteredpixel.shatteredpixeldungeon.scenes.GameScene;
import com.shatteredpixel.shatteredpixeldungeon.scenes.PixelScene;
import com.shatteredpixel.shatteredpixeldungeon.ui.CheckBox;
import com.shatteredpixel.shatteredpixeldungeon.ui.RedButton;
import com.shatteredpixel.shatteredpixeldungeon.ui.RenderedTextBlock;
import com.shatteredpixel.shatteredpixeldungeon.ui.Window;
import com.shatteredpixel.shatteredpixeldungeon.windows.WndTextInput;
import com.watabou.noosa.Game;

public class WndMultiplayer extends Window {
    private float updateTimer = 0;
    private static final float UPDATE_INTERVAL = 0.5f; // Обновляем статус каждые 0.5 секунды
    private static final int WIDTH = 120;
    private static final int HEIGHT = 140;
    private static final float GAP = 2;
    private final CheckBox cbHost;
    private final RedButton btnStartServer;
    private final RedButton btnConnect;
    private final RedButton btnDisconnect;
    private final RedButton btnIP;

    private final RenderedTextBlock statusText;

    public WndMultiplayer() {
        super();

        resize(WIDTH, HEIGHT);

        // Заголовок - используем ключи windows.wndmultiplayer
        RenderedTextBlock title = PixelScene.renderTextBlock(Messages.get("windows.wndmultiplayer.title"), 12);
        title.hardlight(TITLE_COLOR);
        title.setPos((WIDTH - title.width()) / 2, GAP);
        add(title);

        // Be Host
        cbHost = new CheckBox(Messages.get("windows.wndmultiplayer.host")) {
            @Override
            protected void onClick() {
                super.onClick();
                MPSettings.multiplayerHost(checked());
                updateButtons();
                updateStatus();
            }
        };
        float pos = title.bottom() + GAP * 2;
        cbHost.setRect(GAP, pos, WIDTH - GAP * 2, 16);
        cbHost.checked(MPSettings.multiplayerHost());
        add(cbHost);

        // Port Label
        RenderedTextBlock portLabel = PixelScene.renderTextBlock(Messages.get("windows.wndmultiplayer.port"), 8);
        pos = cbHost.bottom() + 3*GAP;
        portLabel.setPos(GAP, pos);
        add(portLabel);

        RedButton btnPort = new RedButton(String.valueOf(MPSettings.multiplayerPort())) {
            @Override
            protected void onClick() {
                WndTextInput wnd = new WndTextInput(
                        Messages.get("windows.wndmultiplayer.port_title"),
                        Messages.get("windows.wndmultiplayer.port_desc"),
                        String.valueOf(MPSettings.multiplayerPort()),
                        5,
                        false,
                        Messages.get("windows.wndmultiplayer.set"),
                        Messages.get("windows.wndmultiplayer.cancel")
                ) {
                    @Override
                    public void onSelect(boolean positive, String text) {
                        if (positive) {
                            try {
                                int port = Integer.parseInt(text);
                                if (port >= 1024 && port <= 65535) {
                                    MPSettings.multiplayerPort(port);
                                    text(String.valueOf(port));
                                }
                            } catch (NumberFormatException e) {
                                // Невалидный порт
                            }
                        }
                    }
                };
                ShatteredPixelDungeon.scene().addToFront(wnd);
            }
        };
        pos = portLabel.centerY() - 8;
        btnPort.setRect(WIDTH - 38, pos, 36, 16);
        add(btnPort);


        RenderedTextBlock ipLabel = PixelScene.renderTextBlock(Messages.get("windows.wndmultiplayer.ip_address"), 8);
        pos = btnPort.bottom() + 6;
        ipLabel.setPos(GAP, pos);
        add(ipLabel);

        btnIP = new RedButton(MPSettings.multiplayerIP()) {
            @Override
            protected void onClick() {
                WndTextInput wnd = new WndTextInput(
                        Messages.get("windows.wndmultiplayer.ip_title"),
                        Messages.get("windows.wndmultiplayer.ip_desc"),
                        MPSettings.multiplayerIP(),
                        15,
                        false,
                        Messages.get("windows.wndmultiplayer.set"),
                        Messages.get("windows.wndmultiplayer.cancel")
                ) {
                    @Override
                    public void onSelect(boolean positive, String text) {
                        if (positive) {
                            MPSettings.multiplayerIP(text);
                            text(text);
                        }
                    }
                };
                ShatteredPixelDungeon.scene().addToFront(wnd);
            }
        };
        pos = btnPort.bottom() + 1;
        btnIP.setRect(WIDTH - 87 + 20, pos, 65, 16);
        add(btnIP);

        // Start Server Button
        btnStartServer = new RedButton(Messages.get("windows.wndmultiplayer.start_server")) {
            @Override
            protected void onClick() {
                if (cbHost.checked()) {
                    NetworkManager.getInstance().startServer();
                    btnStartServer.enable(false);
                    updateButtons();
                }
            }
        };
        pos = btnIP.bottom() + 1;
        btnStartServer.setRect(GAP, pos, WIDTH - GAP * 2, 16);
        add(btnStartServer);

        // Connect Button
        btnConnect = new RedButton(Messages.get("windows.wndmultiplayer.connect")) {
            @Override
            protected void onClick() {
                if (!cbHost.checked()) {
                    NetworkManager.getInstance().connectToServer(MPSettings.multiplayerIP());
                    updateButtons();
                    updateStatus();
                }
            }
        };
        pos = btnStartServer.bottom() + 3*GAP;
        btnConnect.setRect(GAP, pos, WIDTH - GAP * 2, 16);
        add(btnConnect);


        // Disconnect Button
        btnDisconnect = new RedButton(Messages.get("windows.wndmultiplayer.disconnect")) {
            @Override
            protected void onClick() {
                NetworkManager.getInstance().disconnect();
                network.Multiplayer.isMultiplayer = false;
                network.Multiplayer.isHost = false;
                updateButtons();
                updateStatus();
            }
        };
        pos = btnConnect.bottom() + 1;
        btnDisconnect.setRect(GAP, pos, WIDTH - GAP * 2, 16);
        btnDisconnect.enable(false);
        add(btnDisconnect);

        // statusText
        statusText = PixelScene.renderTextBlock("", 6);
        pos = btnDisconnect.bottom() + GAP;
        statusText.setPos(GAP, pos);
        add(statusText);

        updateStatus();
        updateButtons();
    }

    private void updateButtons() {
        NetworkManager manager = NetworkManager.getInstance();
        boolean isConnected = manager.isConnected();

        cbHost.enable(!isConnected);
        btnIP.enable(!isConnected && !cbHost.checked());
        btnStartServer.enable(!isConnected && cbHost.checked());
        btnConnect.enable(!isConnected && !cbHost.checked());
        btnDisconnect.enable(isConnected);
    }

    private void updateStatus() {
        NetworkManager manager = NetworkManager.getInstance();

        if (manager.isConnected()) {
            NetworkManager.Mode mode = manager.getMode();
            if (mode == NetworkManager.Mode.SERVER) {
                statusText.text(Messages.get("windows.wndmultiplayer.status_hosting") +
                        " (" + Multiplayer.Players.getPlayerCount() + " players)");
                statusText.hardlight(0x00FF00);
            } else {
                statusText.text(Messages.get("windows.wndmultiplayer.status_connected"));
                statusText.hardlight(0x00AAFF);
            }
            network.Multiplayer.isMultiplayer = true;
            network.Multiplayer.isHost = (mode == NetworkManager.Mode.SERVER);
        } else {
            statusText.text(Messages.get("windows.wndmultiplayer.status_disconnected"));
            statusText.hardlight(0xFF0000);
            network.Multiplayer.isMultiplayer = false;
            network.Multiplayer.isHost = false;
        }
    }

    @Override
    public void update() {
        super.update();
        // Обновляем статус не каждый кадр, а с интервалом
        updateTimer += Game.elapsed;
        if (updateTimer >= UPDATE_INTERVAL) {
            updateTimer = 0;
            updateStatus();
            updateButtons();
        }
    }
}