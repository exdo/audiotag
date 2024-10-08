package xyz.idaoteng.audiotag.component;

import javafx.application.Platform;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.geometry.Orientation;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Menu;
import javafx.scene.control.MenuItem;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.control.skin.TableHeaderRow;
import javafx.scene.control.skin.TableViewSkin;
import javafx.scene.input.*;
import xyz.idaoteng.audiotag.Session;
import xyz.idaoteng.audiotag.Utils;
import xyz.idaoteng.audiotag.bean.AudioMetaData;
import xyz.idaoteng.audiotag.bean.EditableTag;
import xyz.idaoteng.audiotag.core.MetaDataReader;
import xyz.idaoteng.audiotag.core.MetaDataWriter;
import xyz.idaoteng.audiotag.dialog.*;
import xyz.idaoteng.audiotag.exception.CantReadException;
import xyz.idaoteng.audiotag.notification.Notification;

import java.awt.*;
import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.*;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class Center {
    private static final TableView<AudioMetaData> TABLE_VIEW = new TableView<>();
    private static final ContextMenu CONTEXT_MENU = new ContextMenu();
    private static ScrollBar verticalScrollBar = null;
    private static double horizontalScrollBarHeight;
    private static double tableHeadRowHeight;
    private static final int ABOVE_VIEWPORT = -1;
    private static final int BLOW_VIEWPORT = -2;
    private static final int IN_VIEWPORT_BLANK = 0;
    private static final double ROW_HEIGHT = 25;
    private static final  MenuItem ENABLE_DRAG_ROW_MENU_ITEM = new MenuItem("允许拖拽行");
    private static RadioButton enableDragRowRadioButton = null;
    private static final MenuItem RENAME_MENU_ITEM = new MenuItem("重命名");
    private static final MenuItem OPEN_BY_BROWSER = new MenuItem("打开文件所在的目录");

    private static final HashSet<String> ALTERNATIVE_ARTISTS = new HashSet<>();
    private static final HashSet<String> ALTERNATIVE_ALBUMS = new HashSet<>();

    private static boolean disableDragRow = true;
    private static final String ALLOW = "允许拖拽行";
    private static final String BAN = "禁止拖拽行";

    private static final DataFormat DATA_FORMAT = new DataFormat("application/x-java-serialized-object");
    private static Menu deleteSpecificTagMenu;

    // 初始化表格
    static {
        // 设置表格样式
        TABLE_VIEW.setMinHeight(600); // 600 刚好可以将侧边栏（Aside）中的所有组件显示出来
        // 添加一个右边框，其他几个方向不需要
        TABLE_VIEW.setStyle("-fx-border-style: solid; -fx-border-color: #cccccc; -fx-border-width: 0 1 0 0");

        // 创建列
        createColumn();

        // TableView 的行高不知道的怎么获取，故此手动设置为 ROW_HEIGHT
        TABLE_VIEW.setRowFactory(table -> {
            TableRow<AudioMetaData> row = new TableRow<>();
            row.setMinHeight(ROW_HEIGHT);
            row.setMaxHeight(ROW_HEIGHT);

            makeRowDraggable(row);

            return row;
        });

        // 显示表头最右侧的 + 按钮
        TABLE_VIEW.setTableMenuButtonVisible(true);
        // 允许多选
        TABLE_VIEW.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);

        // 配置右键菜单
        configContextMenu();

        // 配置 重命名按钮 、 打开文件所在的目录按钮 和 删除特定标签菜单项 的 disable 属性
        ObservableList<AudioMetaData> items = TABLE_VIEW.getSelectionModel().getSelectedItems();
        items.addListener((ListChangeListener<AudioMetaData>) listener -> {
            // 重命名按钮只在选中一个时可用
            RENAME_MENU_ITEM.setDisable(listener.getList().size() != 1);

            // 打开文件所在的目录按钮只在选中一个时可用
            OPEN_BY_BROWSER.setDisable(listener.getList().size() != 1);

            deleteSpecificTagMenu.setDisable(items.isEmpty());
        });

        // 初始化内容。在 Session 中记录了上次所展示的内容
        initContent();
    }

    private static void createColumn() {
        TableColumn<AudioMetaData, String> filenameColumn = new TableColumn<>("文件名");
        filenameColumn.setPrefWidth(235);
        filenameColumn.setId("filename");
        filenameColumn.setCellValueFactory(new PropertyValueFactory<>("filename"));
        filenameColumn.setReorderable(false);

        TableColumn<AudioMetaData, String> titleColumn = new TableColumn<>("标题");
        titleColumn.setPrefWidth(175);
        titleColumn.setId("title");
        titleColumn.setCellValueFactory(new PropertyValueFactory<>("title"));

        TableColumn<AudioMetaData, String> artistColumn = new TableColumn<>("艺术家");
        artistColumn.setPrefWidth(150);
        artistColumn.setId("artist");
        artistColumn.setCellValueFactory(new PropertyValueFactory<>("artist"));

        TableColumn<AudioMetaData, String> albumColumn = new TableColumn<>("专辑");
        albumColumn.setPrefWidth(175);
        albumColumn.setId("album");
        albumColumn.setCellValueFactory(new PropertyValueFactory<>("album"));

        TableColumn<AudioMetaData, String> dateColumn = new TableColumn<>("出版日期");
        dateColumn.setPrefWidth(100);
        dateColumn.setId("date");
        dateColumn.setStyle("-fx-alignment: CENTER-RIGHT");
        dateColumn.setCellValueFactory(new PropertyValueFactory<>("date"));

        TableColumn<AudioMetaData, String> genreColumn = new TableColumn<>("流派");
        genreColumn.setPrefWidth(85);
        genreColumn.setId("genre");
        genreColumn.setCellValueFactory(new PropertyValueFactory<>("genre"));

        TableColumn<AudioMetaData, String> trackColumn = new TableColumn<>("音轨序号");
        trackColumn.setPrefWidth(55);
        trackColumn.setId("track");
        trackColumn.setCellValueFactory(new PropertyValueFactory<>("track"));

        TableColumn<AudioMetaData, String> commentColumn = new TableColumn<>("备注");
        commentColumn.setPrefWidth(200);
        commentColumn.setId("comment");
        commentColumn.setCellValueFactory(new PropertyValueFactory<>("comment"));

        TableColumn<AudioMetaData, String> bitrateColumn = new TableColumn<>("比特率");
        bitrateColumn.setPrefWidth(75);
        bitrateColumn.setId("bitrate");
        bitrateColumn.setStyle("-fx-alignment: CENTER-RIGHT");
        bitrateColumn.setCellValueFactory(new PropertyValueFactory<>("bitrate"));

        TableColumn<AudioMetaData, String> lengthColumn = new TableColumn<>("时长");
        lengthColumn.setPrefWidth(65);
        lengthColumn.setId("length");
        lengthColumn.setStyle("-fx-alignment: CENTER-RIGHT");
        lengthColumn.setCellValueFactory(new PropertyValueFactory<>("length"));

        HashMap<String, TableColumn<AudioMetaData, String>> id_column = new HashMap<>(10);
        id_column.put("filename", filenameColumn);
        id_column.put("title", titleColumn);
        id_column.put("artist", artistColumn);
        id_column.put("album", albumColumn);
        id_column.put("date", dateColumn);
        id_column.put("genre", genreColumn);
        id_column.put("track", trackColumn);
        id_column.put("comment", commentColumn);
        id_column.put("bitrate", bitrateColumn);
        id_column.put("length", lengthColumn);

        HashMap<Integer, String> columnsOrder = Session.getColumnsOrder();
        for (int i = 0; i < 10; i++) {
            String id = columnsOrder.get(i);
            TABLE_VIEW.getColumns().add(id_column.get(id));
        }
    }

    private static void makeRowDraggable(TableRow<AudioMetaData> row) {
        //拖拽-检测
        row.setOnDragDetected(event -> {
            if (disableDragRow) return;

            if (!row.isEmpty()) {
                Integer index = row.getIndex();
                Dragboard db = row.startDragAndDrop(TransferMode.MOVE);
                db.setDragView(row.snapshot(null, null));
                ClipboardContent cc = new ClipboardContent();
                cc.put(DATA_FORMAT, index);
                db.setContent(cc);
                event.consume();
            }
        });
        //释放-验证
        row.setOnDragOver(event -> {
            if (disableDragRow) return;

            Dragboard db = event.getDragboard();
            if (db.hasContent(DATA_FORMAT)) {
                if (row.getIndex() != (Integer) db.getContent(DATA_FORMAT)) {
                    event.acceptTransferModes(TransferMode.COPY_OR_MOVE);
                    event.consume();
                }
            }
        });
        //释放-执行
        row.setOnDragDropped(event -> {
            if (disableDragRow) return;

            Dragboard db = event.getDragboard();
            if (db.hasContent(DATA_FORMAT)) {
                int draggedIndex = (Integer) db.getContent(DATA_FORMAT);
                AudioMetaData draggedMetaData = TABLE_VIEW.getItems().remove(draggedIndex);

                int dropIndex;
                if (row.isEmpty()) {
                    dropIndex = TABLE_VIEW.getItems().size();
                } else {
                    dropIndex = row.getIndex();
                }

                TABLE_VIEW.getItems().add(dropIndex, draggedMetaData);

                event.setDropCompleted(true);
                TABLE_VIEW.getSelectionModel().clearSelection();
                TABLE_VIEW.getSelectionModel().select(dropIndex);
                event.consume();
            }
        });
    }

    private static void configContextMenu() {
        MenuItem selectAll = new MenuItem("全选");
        selectAll.setOnAction(event -> selectAll());

        RENAME_MENU_ITEM.setOnAction(event -> Rename.show(TABLE_VIEW.getSelectionModel().getSelectedItem()));

        ENABLE_DRAG_ROW_MENU_ITEM.setOnAction(event -> {
            switch (ENABLE_DRAG_ROW_MENU_ITEM.getText()) {
                case ALLOW -> {
                    enableDragRow(true);
                    enableDragRowRadioButton.setSelected(true);
                    ENABLE_DRAG_ROW_MENU_ITEM.setText(BAN);
                }
                case BAN -> {
                    enableDragRow(false);
                    enableDragRowRadioButton.setSelected(false);
                    ENABLE_DRAG_ROW_MENU_ITEM.setText(ALLOW);
                }
            }
        });

        MenuItem renameBaseOnTags = new MenuItem("根据标签重命名");
        renameBaseOnTags.setOnAction(event -> renameBaseOnTags());

        MenuItem addTagsBaseOnFilename = new MenuItem("基于文件名添加标签");
        addTagsBaseOnFilename.setOnAction(event -> addTagBaseOnFilename());

        MenuItem deleteFromTable = new MenuItem("从表格中移除");
        deleteFromTable.setOnAction(event -> removeSelectedItems());

        MenuItem deleteFile = new MenuItem("删除文件");
        deleteFile.setOnAction(event -> deleteSelectedItems());

        MenuItem addOrder = new MenuItem("从上至下依次添加序号");
        addOrder.setOnAction(event -> addOrder());

        deleteSpecificTagMenu = generateDeleteSpecificTagMenu();

        MenuItem packageToAlbum = new MenuItem("设置成同一专辑");
        packageToAlbum.setOnAction(event -> packageToAlbum());

        OPEN_BY_BROWSER.setOnAction(event -> {
            AudioMetaData audioMetaData = TABLE_VIEW.getSelectionModel().getSelectedItem();
            String path = audioMetaData.getAbsolutePath();
            try {
                Desktop.getDesktop().open(new File(path).getParentFile());
            } catch (IOException e) {
                e.printStackTrace();
            }
        });

        MenuItem cancel = new MenuItem("取消");
        cancel.setOnAction(event -> TABLE_VIEW.getSelectionModel().clearSelection());

        CONTEXT_MENU.getItems().addAll(selectAll,
                ENABLE_DRAG_ROW_MENU_ITEM,
                RENAME_MENU_ITEM,
                renameBaseOnTags,
                addTagsBaseOnFilename,
                deleteFromTable,
                deleteFile,
                addOrder,
                deleteSpecificTagMenu,
                packageToAlbum,
                generateSameAlbumOptionMenu(),
                generateTidyMenu(),
                OPEN_BY_BROWSER,
                cancel);

        TABLE_VIEW.setContextMenu(CONTEXT_MENU);
    }

    public static void selectAll() {
        TABLE_VIEW.getSelectionModel().selectAll();
    }

    private static void enableDragRow(boolean enable) {
        disableDragRow = !enable;
    }

    public static void renameBaseOnTags() {
        List<AudioMetaData> selectedItems = checkSelectedRows();
        if (selectedItems == null) return;

        RenameBaseOnTag.show(selectedItems);
        updateTableView(null);
    }

    public static List<AudioMetaData> checkSelectedRows() {
        List<AudioMetaData> selectedItems = TABLE_VIEW.getSelectionModel().getSelectedItems();
        if (selectedItems.isEmpty()) {
            return NeedToSelectAll.isSelectAll() ? TABLE_VIEW.getItems() : null;
        }
        return selectedItems;
    }

    public static void updateTableView(List<AudioMetaData> dataList) {
        // 如果 dataList 为 null，则刷新当前表格
        if (dataList == null) {
            recordeUpdate(TABLE_VIEW.getItems());
            TABLE_VIEW.refresh();
        } else {
            Aside.showBlank();
            recordeUpdate(dataList);
            TABLE_VIEW.getItems().clear();
            TABLE_VIEW.getItems().addAll(dataList);
            TABLE_VIEW.refresh();
        }
    }

    // 表格刷新后，记录表格内容的变化并生成备选艺术家、备选专辑
    private static void recordeUpdate(List<AudioMetaData> dataList) {
        ALTERNATIVE_ARTISTS.clear();
        ALTERNATIVE_ALBUMS.clear();

        List<String> paths = new ArrayList<>(dataList.size());

        for (AudioMetaData metaData : dataList) {
            String artist = metaData.getArtist();
            if (artist != null) {
                artist = artist.trim();
                if (!"".equals(artist)) {
                    ALTERNATIVE_ARTISTS.add(artist);
                }
            }

            String album = metaData.getAlbum();
            if (album != null) {
                album = album.trim();
                if (!"".equals(album)) {
                    ALTERNATIVE_ALBUMS.add(album);
                }
            }

            paths.add(metaData.getAbsolutePath());
        }

        // 记录当前表格的内容以便下次打开软件时展示
        Session.setCurrentTableViewContentPaths(paths);
    }

    public static List<String> getAlternativeArtists() {
        return new ArrayList<>(ALTERNATIVE_ARTISTS);
    }

    public static List<String> getAlternativeAlbums() {
        return new ArrayList<>(ALTERNATIVE_ALBUMS);
    }
    public static void addTagBaseOnFilename() {
        List<AudioMetaData> selectedItems = checkSelectedRows();
        if (selectedItems == null) return;

        AddTagBaseOnFilename.show(selectedItems);
        updateTableView(null);
    }

    public static void removeSelectedItems() {
        List<AudioMetaData> selectedItems = TABLE_VIEW.getSelectionModel().getSelectedItems();

        if (selectedItems.isEmpty()) {
            if (NeedToSelectAll.isSelectAll()) {
                TABLE_VIEW.getItems().clear();
            }
        } else {
            TABLE_VIEW.getItems().removeAll(selectedItems);
            updateTableView(null);
        }
    }

    public static void deleteSelectedItems() {
        List<AudioMetaData> selectedItems = checkSelectedRows();
        if (selectedItems == null) return;

        List<AudioMetaData> succeed = Delete.show(selectedItems);
        TABLE_VIEW.getItems().removeAll(succeed);
        updateTableView(null);
    }

    public static void addOrder() {
        List<AudioMetaData> selectedItems = checkSelectedRows();
        if (selectedItems == null) return;

        for (int i = 0; i < selectedItems.size(); i++) {
            selectedItems.get(i).setTrack(String.valueOf(i + 1));
        }
        Aside.refresh();
        updateTableView(null);
        Notification.showNotification("序号已添加");
    }

    public static Menu generateDeleteSpecificTagMenu() {
        Menu deleteSpecificTag = new Menu("删除特定标签");

        MenuItem deleteTitle = new MenuItem("删除标题");
        deleteTitle.setOnAction(event -> deleteTag(EditableTag.TITLE));

        MenuItem deleteArtist = new MenuItem("删除艺术家");
        deleteArtist.setOnAction(event -> deleteTag(EditableTag.ARTIST));

        MenuItem deleteAlbum = new MenuItem("删除专辑");
        deleteAlbum.setOnAction(event -> deleteTag(EditableTag.ALBUM));

        MenuItem deleteDate = new MenuItem("删除出版日期");
        deleteDate.setOnAction(event -> deleteTag(EditableTag.DATE));

        MenuItem deleteGenre = new MenuItem("删除流派");
        deleteGenre.setOnAction(event -> deleteTag(EditableTag.GENRE));

        MenuItem deleteTrack = new MenuItem("删除音轨序号");
        deleteTrack.setOnAction(event -> deleteTag(EditableTag.TRACK));

        MenuItem deleteComment = new MenuItem("删除备注");
        deleteComment.setOnAction(event -> deleteTag(EditableTag.COMMENT));

        MenuItem deleteCover = new MenuItem("删除封面");
        deleteCover.setOnAction(event -> deleteTag(EditableTag.COVER));

        MenuItem deleteAll = new MenuItem("删除全部标签");
        deleteAll.setOnAction(event -> deleteTag(EditableTag.ALL));

        deleteSpecificTag.getItems().addAll(deleteTitle, deleteArtist, deleteAlbum, deleteDate,
                deleteGenre, deleteTrack, deleteComment, deleteCover, deleteAll);
        return deleteSpecificTag;
    }

    private static void deleteTag(EditableTag tag) {
        ObservableList<AudioMetaData> items = TABLE_VIEW.getSelectionModel().getSelectedItems();

        if (items.isEmpty()) {
            return;
        }

        for (AudioMetaData item : items) {
            switch (tag) {
                case TITLE -> item.setTitle("");
                case ARTIST -> item.setArtist("");
                case ALBUM -> item.setAlbum("");
                case DATE -> item.setDate("");
                case GENRE -> item.setGenre("");
                case TRACK -> item.setTrack("");
                case COMMENT -> item.setComment("");
                case COVER -> item.setCover(null);
                case ALL -> {
                    item.setTitle("");
                    item.setArtist("");
                    item.setAlbum("");
                    item.setDate("");
                    item.setGenre("");
                    item.setTrack("");
                    item.setComment("");
                    item.setCover(null);
                }
            }
            MetaDataWriter.write(item);
        }

        Aside.refresh();
        updateTableView(null);
        Notification.showNotification("删除完毕");
    }

    public static void packageToAlbum() {
        List<AudioMetaData> selectedItems = checkSelectedRows();
        if (selectedItems == null) return;

        String albumName = PackageToAlbum.show();
        if (albumName == null) return;

        for (AudioMetaData selectedItem : selectedItems) {
            selectedItem.setAlbum(albumName);
            MetaDataWriter.write(selectedItem);
        }
        Aside.refresh();
        updateTableView(null);
        Notification.showNotification("已设置为同一专辑");
    }

    public static Menu generateSameAlbumOptionMenu() {
        Menu menu = new Menu("批量设置专辑");

        MenuItem addCover = new MenuItem("为同一专辑添加同一封面");
        addCover.setOnAction(event -> addCoverForSameAlbum());

        MenuItem addArtist = new MenuItem("为同一专辑添加同一艺术家");
        addArtist.setOnAction(event -> addArtistForSameAlbum());

        MenuItem addGenre = new MenuItem("为同一专辑添加同一流派");
        addGenre.setOnAction(event -> addGenreForSameAlbum());

        MenuItem addDate = new MenuItem("为同一专辑添加同一出版日期");
        addDate.setOnAction(event -> addDateForSameAlbum());

        menu.getItems().addAll(addCover, addArtist, addGenre, addDate);

        return menu;
    }

    public static void addCoverForSameAlbum() {
        List<AudioMetaData> selectedItems = checkSelectedRows();
        if (selectedItems == null) return;
        // albumCovers 专辑 -> 封面 映射表
        HashMap<String, byte[]> albumCovers = new HashMap<>();
        for (AudioMetaData selectedItem : selectedItems) {
            String album = selectedItem.getAlbum();
            byte[] cover = selectedItem.getCover();
            if (!"".equals(album) && cover != null) {
                cover = Utils.retouchCover(cover);
                albumCovers.put(album, cover);
            }
        }

        for (AudioMetaData selectedItem : selectedItems) {
            String album = selectedItem.getAlbum();
            if (!"".equals(album)) {
                // 根据 专辑 找到 封面
                selectedItem.setCover(albumCovers.get(album));
                MetaDataWriter.write(selectedItem);
            }
        }
        Aside.refresh();
        updateTableView(null);
        Notification.showNotification("专辑封面更新完毕");
    }

    public static void addArtistForSameAlbum() {
        List<AudioMetaData> selectedItems = checkSelectedRows();
        if (selectedItems == null) return;
        // albumArtist 专辑 -> 艺术家 映射表
        HashMap<String, String> albumArtist = new HashMap<>();
        for (AudioMetaData selectedItem : selectedItems) {
            String album = selectedItem.getAlbum();
            String artist = selectedItem.getArtist();
            if (!"".equals(album) && !"".equals(artist)) {
                albumArtist.put(album, artist);
            }
        }

        for (AudioMetaData selectedItem : selectedItems) {
            String album = selectedItem.getAlbum();
            if (!"".equals(album)) {
                // 根据 专辑 找到 艺术家
                selectedItem.setArtist(albumArtist.get(album));
            }
        }
        Aside.refresh();
        updateTableView(null);
        Notification.showNotification("专辑艺术家更新完毕");
    }

    private static void addGenreForSameAlbum() {
        List<AudioMetaData> selectedItems = checkSelectedRows();
        if (selectedItems == null) return;
        // albumGenre 专辑 -> 流派 映射表
        HashMap<String, String> albumGenre = new HashMap<>();
        for (AudioMetaData item : selectedItems) {
            String album = item.getAlbum();
            String genre = item.getGenre();
            if (!"".equals(album) && !"".equals(genre)) {
                albumGenre.put(album, genre);
            }
        }

        for (AudioMetaData item : selectedItems) {
            String album = item.getAlbum();
            if (!"".equals(album)) {
                // 根据 专辑 找到 流派
                item.setGenre(albumGenre.get(album));
                MetaDataWriter.write(item);
            }
        }
        Aside.refresh();
        updateTableView(null);
        Notification.showNotification("专辑流派更新完毕");
    }

    private static void addDateForSameAlbum() {
        List<AudioMetaData> selectedItems = checkSelectedRows();
        if (selectedItems == null) return;
        // albumDate 专辑 -> 发行日期 映射表
        HashMap<String, String> albumDate = new HashMap<>();
        for (AudioMetaData selectedItem : selectedItems) {
            String album = selectedItem.getAlbum();
            String date = selectedItem.getDate();
            if (!"".equals(album) && !"".equals(date)) {
                albumDate.put(album, date);
            }
        }

        for (AudioMetaData selectedItem : selectedItems) {
            String album = selectedItem.getAlbum();
            if (!"".equals(album)) {
                // 根据 专辑 找到 发行日期
                selectedItem.setDate(albumDate.get(album));
                MetaDataWriter.write(selectedItem);
            }
        }
        Aside.refresh();
        updateTableView(null);
        Notification.showNotification("专辑发行日期更新完毕");
    }

    public static Menu generateTidyMenu() {
        Menu tidyMenu = new Menu("整理");

        MenuItem tidyByArtist = new MenuItem("将同一歌手的文件放置在同一文件夹");
        tidyByArtist.setOnAction(event -> {
            List<AudioMetaData> dataList = checkSelectedRows();
            if (dataList == null) return;

            Tidy.show(dataList, true);
        });

        MenuItem tidyByAlbum = new MenuItem("将同一专辑的文件放置在同一文件夹");
        tidyByAlbum.setOnAction(event -> {
            List<AudioMetaData> dataList = checkSelectedRows();
            if (dataList == null) return;

            Tidy.show(dataList, false);
        });

        tidyMenu.getItems().addAll(tidyByArtist, tidyByAlbum);
        return tidyMenu;
    }

    private static void initContent() {
        List<String> paths = Session.getCurrentTableViewContentPaths();
        List<AudioMetaData> audioMetaData = new ArrayList<>(paths.size());
        paths.forEach(path -> {
            try {
                audioMetaData.add(MetaDataReader.readFile(new File(path)));
            } catch (CantReadException ignored) {}
        });
        updateTableView(audioMetaData);
    }

    public static void configWhenTableAlreadyRendered() {
        // 为实现拖选，需要先获取表头的高度
        // 表头的高度需要 skin 已经渲染完毕时才能获取
        TableViewSkin<?> skin = (TableViewSkin<?>) TABLE_VIEW.getSkin();
        ObservableList<Node> childrenList = skin.getChildren();
        for (Node node : childrenList) {
            if (node instanceof TableHeaderRow headerRow) {
                tableHeadRowHeight = headerRow.getHeight();
            }
        }

        // 获取并配置滚动条，为实现拖选做准备
        getAndConfigScrollBar();
        // 实现拖选功能
        implementDragAndSelect();
    }

    private static void getAndConfigScrollBar() {
        Set<Node> nodes = TABLE_VIEW.lookupAll(".scroll-bar");
        for (Node node : nodes) {
            if (node instanceof ScrollBar scrollBar) {
                if (scrollBar.getOrientation().equals(Orientation.VERTICAL)) {
                    verticalScrollBar = scrollBar;
                }

                // 监听横向滚动条的高度（经测试横向滚动条的高度是会变化的）
                if (scrollBar.getOrientation().equals(Orientation.HORIZONTAL)) {
                    scrollBar.heightProperty().addListener((ob, o, n) -> horizontalScrollBarHeight = n.doubleValue());
                }
            }
        }
    }

    // 在 javafx 中一个完整的拖动事件也是单击事件
    // 鼠标是否进行过拖动：用来确认是到底是拖动还是单击
    private static boolean mouseDragged = false;
    // 起始行号：鼠标按下时的行号
    private static Integer indexWhenDragStart = null;
    // 鼠标拖动结束时最后被选中的行的行号，对应行的内容将展示在侧边栏
    private static Integer indexOfLastSelectedRow = null;
    public static void implementDragAndSelect() {
        TABLE_VIEW.setOnMouseClicked(event -> {
            // 处理单击事件
            if (event.getClickCount() == 1) {
                if (event.getButton().equals(MouseButton.PRIMARY)) {
                    handlerPrimaryButtonClick(event);
                }

                if (event.getButton().equals(MouseButton.SECONDARY)) {
                    handlerSecondaryButtonClick(event);
                }
            }

            // 处理双击事件
            if (event.getClickCount() == 2) {
                if (event.getButton().equals(MouseButton.PRIMARY)) {
                    // 获取当前行号
                    int currentIndex = getItemIndex(event.getY());
                    if (currentIndex > 0) {
                        // 获取当前行
                        AudioMetaData currentItem = TABLE_VIEW.getItems().get(currentIndex - 1);
                        // 将当前行选中
                        selectItem(currentItem);
                        // 使用系统默认方式打开文件
                        try {
                            Desktop.getDesktop().open(new File(currentItem.getAbsolutePath()));
                        } catch (IOException e) {
                            throw new RuntimeException(e);
                        }
                    }
                }
            }
        });

        // 鼠标左键按下/拖动开始的时候，记录其位置对应的行号
        TABLE_VIEW.setOnMousePressed(event -> {
            // 如果是在视口以外的地方按下该语句不会执行， 所以 indexWhenDragStart 不会等于 -1
            // 如果该值为 0 ，则表示鼠标按下的时候没有选中某一行（鼠标从空白处按下）
            if (event.getButton().equals(MouseButton.PRIMARY)) {
                indexWhenDragStart = getItemIndex(event.getY());
            }
        });

        TABLE_VIEW.setOnMouseDragged(Center::handlerMouseDragged);

        TABLE_VIEW.setOnMouseReleased(event -> stopScrollBarAutoScrollThread());
    }

    // 根据鼠标位置计算出该位置对应的实际行号
    // 如果有对应的行，返回的行号大于0（行号从1开始）
    // 否则返回 ABOVE_VIEWPORT（-1：在视口上方）
    //      或 BLOW_VIEWPORT（-2：在视口下方）
    //      或 IN_VIEWPORT_BLANK（0：在视口内的空白处）
    // currentY：鼠标位置的 y 坐标（通过 event.getY() 得到的值）
    private static int getItemIndex(double currentY) {
        // 视口底部的 y 坐标
        double upperLimitY = TABLE_VIEW.getHeight() - horizontalScrollBarHeight;
        // 当鼠标位于视口上方，返回 ABOVE_VIEWPORT
        if (currentY <= tableHeadRowHeight) {
            return ABOVE_VIEWPORT;
        }
        // 当鼠标位于视口下方，返回 BLOW_VIEWPORT
        if (currentY >= upperLimitY) {
            return BLOW_VIEWPORT;
        }

        // 内容的实际高度（以内容的顶部为原点，越向下，高度值递增，行号递增）
        double contentHeight = ROW_HEIGHT * TABLE_VIEW.getItems().size();
        if (verticalScrollBar.isVisible()) { // 垂直滚动条可见时行高的计算方式
            // 除视口外的高度 = 表头的高度 + 水平滚动条的高度
            // 视口的高度 = 表格的高度 - 除视口外的高度
            double heightOutsideContentSection = tableHeadRowHeight + horizontalScrollBarHeight;
            double viewportHeight = TABLE_VIEW.getHeight() - heightOutsideContentSection;
            // 最大偏移量 = 内容的实际高度 - 视口的高度
            double maxOffset = contentHeight - viewportHeight;
            // 偏移量 = 最大偏移量 * 滚动条的偏移比例
            double proportion = verticalScrollBar.getValue() / verticalScrollBar.getMax();
            double offset = maxOffset * proportion;
            // total: 鼠标相对于内容的实际高度
            double total = offset + (currentY - tableHeadRowHeight);
            // 行号 = 鼠标相对于内容的实际高度 / 行高 （向上取整）
            return (int)Math.ceil(total / ROW_HEIGHT);
        } else {
            // 垂直滚动条不可见时行高的计算方式
            double validY = contentHeight + tableHeadRowHeight;
            if (currentY > validY) {
                return IN_VIEWPORT_BLANK;
            } else {
                return (int)Math.ceil((currentY - tableHeadRowHeight) / ROW_HEIGHT);
            }
        }
    }

    private static void handlerPrimaryButtonClick(MouseEvent event) {
        // 如果是拖动行为，则不执行下面的左键单击逻辑
        if (mouseDragged) {
            mouseDragged = false;
            return;
        }

        int itemIndex = getItemIndex(event.getY());
        if (itemIndex > 0) {
            Aside.showMetaData(TABLE_VIEW.getItems().get(itemIndex - 1));
        } else {
            TABLE_VIEW.getSelectionModel().clearSelection();
            Aside.showBlank();
        }
        // 左键单击或拖选结束，重置拖动起点
        indexWhenDragStart = null;
    }

    private static void handlerSecondaryButtonClick(MouseEvent event) {
        int itemIndex = getItemIndex(event.getY());
        if (itemIndex > 0) { // 非空白处右键单击
            // 在侧边栏中显示该行数据
            Aside.showMetaData(TABLE_VIEW.getItems().get(itemIndex - 1));
        } else { // 空白处右键单击
            // 侧边栏显示空白
            Aside.showBlank();
        }
    }

    // 实现框选功能
    private static void handlerMouseDragged(MouseEvent event) {
        // 右键拖动不处理
        if (event.getButton().equals(MouseButton.SECONDARY)) return;

        mouseDragged = true;

        // 视口底部的 y 坐标
        double upperLimitY = TABLE_VIEW.getHeight() - horizontalScrollBarHeight;
        // 结束行号：鼠标拖动到某位置时对应的行号
        int indexWhenDragged;
        if (verticalScrollBar.isVisible()) {
            if (event.getY() >= upperLimitY) {
                // 鼠标拖动到位于视口下方时，自动向下滚动
                verticalScrollBar.increment();
                // 获取行号时将鼠标的位置调整为视口底部偏上一点点
                indexWhenDragged = getItemIndex(upperLimitY - 0.1);
                int start = Math.min(indexWhenDragStart, indexWhenDragged);
                int end = Math.max(indexWhenDragStart, indexWhenDragged);
                selectIndices(start, end);
                // 此时最后选中的行号是鼠标拖动到的行号
                Aside.showMetaData(TABLE_VIEW.getItems().get(indexWhenDragged - 1));
                // 如果鼠标在视口下方按下鼠标不动，则接着自动向下滚动
                moveScrollBarUp = false;
                startScrollBarAutoScrollThread();
            } else if (event.getY() <= tableHeadRowHeight) {
                // tableHeadRowHeight等同于视口顶部的 y 坐标
                // 鼠标拖动到位于视口上方时，自动向上滚动
                verticalScrollBar.decrement();
                // 获取行号时将鼠标的位置调整为视口顶部偏下一点点
                indexWhenDragged = getItemIndex(tableHeadRowHeight + 0.1);
                int start = Math.min(indexWhenDragStart, indexWhenDragged);
                int end = Math.max(indexWhenDragStart, indexWhenDragged);
                selectIndices(start, end);
                // 此时最后选中的行号是鼠标拖动到的行号
                Aside.showMetaData(TABLE_VIEW.getItems().get(indexWhenDragged - 1));
                // 如果鼠标在视口上方按下鼠标不动，则接着自动向上滚动
                moveScrollBarUp = true;
                startScrollBarAutoScrollThread();
            } else {
                // 出现了滚动条，但鼠标没有拖动到视口外面，
                // 此时鼠标已经回到了视口中，应当停止滚动条自动滚动
                stopScrollBarAutoScrollThread();
                // 此时鼠标的位置必定对应着某一行，
                // 故此直接获取鼠标位置对应的行号
                indexWhenDragged = getItemIndex(event.getY());
                int start = Math.min(indexWhenDragStart, indexWhenDragged);
                int end = Math.max(indexWhenDragStart, indexWhenDragged);
                selectIndices(start, end);
                // 此时最后选中的行号是鼠标拖动到的行号
                Aside.showMetaData(TABLE_VIEW.getItems().get(indexWhenDragged - 1));
            }
        } else {
            // 实际内容高度没有超过视口的高度，
            // 此时鼠标的位置可能在某一行上，也可能在视口内的空白处，也可能在视口外面
            // 下面会进行分类讨论
            indexWhenDragged = getItemIndex(event.getY());

            // 从某一行开始
            if (indexWhenDragStart > 0) {
                // 到某一行结束
                if (indexWhenDragged > 0) {
                    int start = Math.min(indexWhenDragStart, indexWhenDragged);
                    int end = Math.max(indexWhenDragStart, indexWhenDragged);
                    selectIndices(start, end);
                    // 此时最后选中的行号是鼠标拖动到的行号
                    indexOfLastSelectedRow = indexWhenDragged;
                }
                // 到视口空白处或视口下方结束
                if (indexWhenDragged == IN_VIEWPORT_BLANK || indexWhenDragged == BLOW_VIEWPORT) {
                    int start = indexWhenDragStart;
                    int end = TABLE_VIEW.getItems().size();
                    selectIndices(start, end);
                    // 此时最后选中的行号固定就是表格的最后一行
                    indexOfLastSelectedRow = end;
                }
                // 到视口上方结束
                if (indexWhenDragged == ABOVE_VIEWPORT) {
                    int start = 1;
                    int end = indexWhenDragStart;
                    selectIndices(start, end);
                    // 此时最后选中的行号固定就是表格的第一行
                    indexOfLastSelectedRow = 1;
                }
            }

            // 从视口空白处开始（空白处必然在在视口下部）
            if (indexWhenDragStart == IN_VIEWPORT_BLANK) {
                // 到某一行结束
                if (indexWhenDragged > 0) {
                    int end = TABLE_VIEW.getItems().size();
                    selectIndices(indexWhenDragged, end);
                    // 此时最后选中的行号是鼠标拖动到的行号
                    indexOfLastSelectedRow = indexWhenDragged;
                }
                // 到视口空白处或视口下方结束
                if (indexWhenDragged == IN_VIEWPORT_BLANK || indexWhenDragged == BLOW_VIEWPORT) {
                    TABLE_VIEW.getSelectionModel().clearSelection();
                    indexOfLastSelectedRow = null;
                }
                // 到视口上方结束（全选）
                if (indexWhenDragged == ABOVE_VIEWPORT) {
                    TABLE_VIEW.getSelectionModel().selectAll();
                    indexOfLastSelectedRow = 1;
                }
            }

            if (indexOfLastSelectedRow != null) {
                Aside.showMetaData(TABLE_VIEW.getItems().get(indexOfLastSelectedRow - 1));
            }
            // 重置变量
            indexOfLastSelectedRow = null;
        }
    }

    // 选中指定范围内的行
    private static void selectIndices(int start, int end) {
        TABLE_VIEW.getSelectionModel().clearSelection();
        for (int i = start; i <= end; i++) {
            TABLE_VIEW.getSelectionModel().selectIndices(i - 1);
        }
    }

    private static ScheduledExecutorService executorService;
    // 滚动条是否在向上滚动
    private static boolean moveScrollBarUp;
    public static void startScrollBarAutoScrollThread() {
        // 定时任务存在时不再创建新的
        if (executorService != null) return;

        executorService = Executors.newSingleThreadScheduledExecutor();
        executorService.scheduleAtFixedRate(() -> Platform.runLater(() -> {
            int indexWhenDragged;
            if (moveScrollBarUp) {
                verticalScrollBar.decrement();
                // 获取行号时将鼠标的位置调整为视口顶部偏下一点点
                indexWhenDragged = getItemIndex(tableHeadRowHeight + 0.1);
            } else {
                verticalScrollBar.increment();
                // 获取行号时将鼠标的位置调整为视口底部偏上一点点
                double upperLimitY = TABLE_VIEW.getHeight() - horizontalScrollBarHeight;
                indexWhenDragged = getItemIndex(upperLimitY - 0.1);
            }
            // 当有滚动条时，经过上面的调整，框选动作总是从某一行开始到某一行结束
            int start = Math.min(indexWhenDragStart, indexWhenDragged);
            int end = Math.max(indexWhenDragStart, indexWhenDragged);
            selectIndices(start, end);

            Aside.showMetaData(TABLE_VIEW.getItems().get(indexWhenDragged - 1));
        }), 0, 70, TimeUnit.MILLISECONDS); // 每隔70毫秒执行一次
    }

    public static void stopScrollBarAutoScrollThread() {
        if (executorService != null) {
            // 强制终止
            executorService.shutdownNow();
            executorService = null;
        }
    }

    public static Node getCenter() {
        return TABLE_VIEW;
    }

    public static void selectItem(AudioMetaData audioMetaData) {
        int index = TABLE_VIEW.getItems().indexOf(audioMetaData);
        TABLE_VIEW.getSelectionModel().select(index);
    }

    public static void takeOverRenameButton(Button rename) {
        rename.disableProperty().bind(RENAME_MENU_ITEM.disableProperty());

        rename.setOnAction(event -> Rename.show(TABLE_VIEW.getSelectionModel().getSelectedItem()));
    }

    public static void takeOverEnableDragRow(RadioButton radioButton) {
        enableDragRowRadioButton = radioButton;
        enableDragRowRadioButton.setOnAction(event -> {
            boolean enable = enableDragRowRadioButton.isSelected();
            if (enable) {
                enableDragRow(true);
                ENABLE_DRAG_ROW_MENU_ITEM.setText(BAN);
            } else {
                enableDragRow(false);
                ENABLE_DRAG_ROW_MENU_ITEM.setText(ALLOW);
            }
        });
    }

    public static void configDeleteSpecificTagMenu(Menu menu) {
        menu.disableProperty().bind(deleteSpecificTagMenu.disableProperty());
    }

    private static final ArrayList<AudioMetaData> BEFORE_FILTERING = new ArrayList<>();
    public static void filter() {
        List<AudioMetaData> before = TABLE_VIEW.getItems();
        BEFORE_FILTERING.addAll(before);
        Filter.show(before);
    }

    public static void turnOffFilter() {
        if (BEFORE_FILTERING.isEmpty()) return;
        updateTableView(BEFORE_FILTERING);
        BEFORE_FILTERING.clear();
    }

    public static HashMap<Integer, String> getColumnOrder() {
        HashMap<Integer, String> columnOrder = new HashMap<>();
        ObservableList<TableColumn<AudioMetaData, ?>> columns = TABLE_VIEW.getColumns();
        for (int i = 0; i < columns.size(); i++) {
            columnOrder.put(i, columns.get(i).getId());
        }
        return columnOrder;
    }
}
