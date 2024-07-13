/*
 * Aoba Hacked Client
 * Copyright (C) 2019-2024 coltonk9043
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package net.aoba.gui;

import net.aoba.Aoba;
import net.aoba.event.events.MouseClickEvent;
import net.aoba.event.events.MouseMoveEvent;
import net.aoba.event.listeners.MouseClickListener;
import net.aoba.event.listeners.MouseMoveListener;
import net.aoba.gui.tabs.components.Component;
import net.aoba.settings.SettingManager;
import net.aoba.settings.types.RectangleSetting;
import net.aoba.utils.types.MouseAction;
import net.aoba.utils.types.MouseButton;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;

import java.util.ArrayList;

public abstract class AbstractGui implements IGuiElement, MouseClickListener, MouseMoveListener {
    protected static MinecraftClient mc = MinecraftClient.getInstance();

    protected String ID;

    protected RectangleSetting position;
    protected Float minWidth = 180.0f;
    protected Float minHeight = 50.0f;
    protected Float maxWidth = null;
    protected Float maxHeight = null;

    protected boolean isMouseOver = false;
    public boolean moveable = true;
    public boolean resizeable = true;
    protected Direction grabDirection = Direction.None;
    protected boolean alwaysVisible = false;
    protected boolean visible = false;

    // Mouse Variables
    protected boolean inheritHeightFromChildren = true;

    protected ArrayList<Component> children = new ArrayList<>();
    protected boolean isMoving = false;
    protected boolean isResizing = false;

    public AbstractGui(String ID, float x, float y, float width, float height) {
        this.ID = ID;
        this.position = new RectangleSetting(ID + "_position", ID + "Position", new Rectangle(x, y, width, height),
                (Rectangle vec) -> UpdateAll(vec));
        SettingManager.registerSetting(position, Aoba.getInstance().settingManager.configContainer);
    }

    public void UpdateAll(Rectangle vec) {
        for (Component component : this.children) {
            component.onParentChanged();
        }
    }

    public String getID() {
        return ID;
    }

    @Override
    public Rectangle getSize() {
        Rectangle position = this.position.getValue();
        return position;
    }

    @Override
    public Rectangle getActualSize() {
        Rectangle position = this.position.getValue();
        return position;
    }

    @Override
    public void setSize(Rectangle size) {
        this.position.setValue(size);
    }

    public void setX(float x) {
        position.setX(x);
    }

    public void setY(float y) {
        position.setY(y);
    }

    public void setWidth(float width) {
        position.setWidth(width);
    }

    public void setHeight(float height) {
        position.setHeight(height);
    }

    @Override
    public void onParentChanged() {
        // Do nothing because this should always be top level!!!
    }

    @Override
    public void onChildChanged(IGuiElement child) {
        // Do nothing...
    }

    @Override
    public void onVisibilityChanged() {
        // Do nothing...
    }

    @Override
    public void onChildAdded(IGuiElement child) {
        // Do nothing...
    }

    public boolean getVisible() {
        return this.visible;
    }

    public void setVisible(boolean state) {
        if (alwaysVisible)
            state = true;

        if (visible != state) {
            this.visible = state;
            for (Component component : children) {
                component.setVisible(state);
            }

            // Binds/Unbinds respective listeners depending on whether it is visible.
            if (state) {
                Aoba.instance.eventManager.AddListener(MouseClickListener.class, this);
                Aoba.instance.eventManager.AddListener(MouseMoveListener.class, this);
            } else {
                Aoba.instance.eventManager.RemoveListener(MouseClickListener.class, this);
                Aoba.instance.eventManager.RemoveListener(MouseMoveListener.class, this);
            }
        }
    }

    public void setAlwaysVisible(boolean state) {
        this.alwaysVisible = state;
        if (this.alwaysVisible) {
            this.setVisible(true);
        }
    }

    public abstract void update();

    public abstract void draw(DrawContext drawContext, float partialTicks);

    @Override
    public void OnMouseClick(MouseClickEvent event) {
        if (event.button == MouseButton.LEFT && event.action == MouseAction.DOWN) {
            float mouseX = (float) event.mouseX;
            float mouseY = (float) event.mouseY;

            Rectangle pos = position.getValue();

            if (Aoba.getInstance().hudManager.isClickGuiOpen() && GuiManager.currentGrabbed == null) {
                if (resizeable) {
                    Rectangle topHitbox = new Rectangle(pos.getX(), pos.getY() - 8, pos.getWidth(), 8.0f);
                    Rectangle leftHitbox = new Rectangle(pos.getX() - 8, pos.getY(), 8.0f, pos.getHeight());
                    Rectangle rightHitbox = new Rectangle(pos.getX() + pos.getWidth(), pos.getY(), 8.0f, pos.getHeight());
                    Rectangle bottomHitbox = new Rectangle(pos.getX(), pos.getY() + pos.getHeight(), pos.getWidth(), 8.0f);

                    if (leftHitbox.intersects(mouseX, mouseY))
                        setResizing(true, mouseX, mouseY, Direction.Left);
                    else if (rightHitbox.intersects(mouseX, mouseY))
                        setResizing(true, mouseX, mouseY, Direction.Right);
                    else if (topHitbox.intersects(mouseX, mouseY))
                        setResizing(true, mouseX, mouseY, Direction.Top);
                    else if (bottomHitbox.intersects(mouseX, mouseY))
                        setResizing(true, mouseX, mouseY, Direction.Bottom);
                    else
                        setResizing(false, mouseX, mouseY, Direction.None);
                }

                if (moveable && !isResizing) {
                    if (mouseX >= pos.getX() && mouseX <= (pos.getX() + pos.getWidth()) && mouseY >= pos.getY()
                            && mouseY <= (pos.getY() + pos.getHeight())) {
                        GuiManager.currentGrabbed = this;
                        isMoving = true;
                    }
                }
            }
        } else if (event.button == MouseButton.LEFT && event.action == MouseAction.UP) {
            isMoving = false; // Handle mouse release
            isResizing = false;
        }
    }


    protected void setResizing(boolean state, double mouseX, double mouseY, Direction direction) {
        if (state)
            GuiManager.currentGrabbed = this;

        isResizing = state;
        grabDirection = direction;
    }

    @Override
    public void OnMouseMove(MouseMoveEvent event) {
        if (this.visible) {
            double mouseX = event.getX();
            double mouseY = event.getY();
            double mouseDeltaX = event.getDeltaX();
            double mouseDeltaY = event.getDeltaY();

            Rectangle pos = position.getValue();

            if (GuiManager.currentGrabbed == this) {
                if (this.isMoving) {
                    this.setX(this.getSize().getX() + (float) mouseDeltaX);
                    this.setY(this.getSize().getY() + (float) mouseDeltaY);
                } else if (this.isResizing) {
                    switch (grabDirection) {
                        case Direction.Top:
                            float newHeightTop = getSize().getHeight() - (float) mouseDeltaY;

                            if (minHeight != null && newHeightTop < minHeight.floatValue())
                                break;

                            if (maxHeight != null && newHeightTop > maxHeight.floatValue())
                                break;

                            setY(getSize().getY() + (float) mouseDeltaY);
                            setHeight(newHeightTop);

                            break;
                        case Direction.Bottom:
                            float newHeightBottom = getSize().getHeight() + (float) mouseDeltaY;

                            if (minHeight != null && newHeightBottom < minHeight.floatValue())
                                break;

                            if (maxHeight != null && newHeightBottom > maxHeight.floatValue())
                                break;

                            setHeight(newHeightBottom);
                            break;
                        case Direction.Left:
                            float newWidthLeft = getSize().getWidth() - (float) mouseDeltaX;
                            if (minWidth != null && newWidthLeft < minWidth.floatValue())
                                break;

                            if (maxWidth != null && newWidthLeft > maxWidth.floatValue())
                                break;

                            setX(getSize().getX() + (float) mouseDeltaX);
                            setWidth(newWidthLeft);
                            break;
                        case Direction.Right:
                            float newWidthRight = getSize().getWidth() + (float) mouseDeltaX;
                            if (minWidth != null && newWidthRight < minWidth.floatValue())
                                break;

                            if (maxWidth != null && newWidthRight > maxWidth.floatValue())
                                break;

                            setWidth(newWidthRight);
                            break;
                        default:
                            break;
                    }
                }
            }

            if (mouseX >= pos.getX() && mouseX <= pos.getX() + pos.getWidth()) {
                if (mouseY >= pos.getY() && mouseY <= pos.getY() + pos.getHeight()) {
                    isMouseOver = true;
                } else {
                    isMouseOver = false;
                }
            } else {
                isMouseOver = false;
            }
        } else {
            isMouseOver = false;
        }
    }
}