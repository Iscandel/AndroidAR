package com.ar.renderer;

import org.opencv.core.Mat;

/**
 * Base class for renderer
 */
public interface ARRenderer {
    public enum ModelType {
        FOX,
        DONA
    }

    /**
     * Affects the intrinsic parameters matrix
     * @param K The matrix to set
     */
    void setIntrinsicParamsMatrix(Mat K);

    /**
     * Affects the extrinsic parameters matrix
     * @param translation The translation vector
     * @param rotation The rotation matrix
     */
    void setExtrinsicMatrix(Mat translation, Mat rotation);

    /**
     * Renders the scene
     * @param frame Frame to draw on
     * @param homography Computed homography matrix
     * @param proj Computed projection matrix
     * @param imgRefWidth Width value of the reference image
     * @param imgRefHeight Height value of the reference image
     * @return
     */
    Mat render(Mat frame, Mat homography, Mat proj, int imgRefWidth, int imgRefHeight);

    /**
     * Indicates whether we should draw the 3D model. The model will be effectively drawn if
     * @see{askDrawing} has been set to true.
     * @param draw True to draw, false otherwise
     */
    void shouldDrawModel(boolean draw);

    /**
     * Indicates whether we should draw the coordinate system. The frame will be effectively drawn
     * if @see{askDrawing} has been set to true.
     * @param draw True to draw, false otherwise
     */
    void shouldDrawFrame(boolean draw);

    /**
     * Indicates whether we should draw the border. The model will be effectively drawn if
     * @see{askDrawing} has been set to true.
     * @param draw True to draw, false otherwise
     */
    void shouldDrawBorder(boolean draw);

    /**
     * Indicates whether we should draw the scene. Generally, the scene should not be drawn
     * if the homography matrix could not be computed.
     * @param ask True to draw, false otherwise
     */
    void askDrawing(boolean ask);

    /**
     * Returns whether the border should be drawn
     * @return True if the border should be drawn, false otherwise
     */
    boolean isDrawBorder();

    /**
     * Returns whether the frame should be drawn
     * @return True if the frame should be drawn, false otherwise
     */
    boolean isDrawFrame();

    /**
     * Returns whether the model should be drawn
     * @return True if the model should be drawn, false otherwise
     */
    boolean isDrawModel();

    /**
     * Defines 3D the model to display
     * @param type Type of 3D model to load
     */
    void setModel(ModelType type);

    /**
     * Defines the scale of the model
     * @param x x value
     * @param y y value
     * @param z z value
     */
    void setModelScale(double x, double y, double z);

    /**
     * Defines the position of the model applying a translation compared to its barycenter.
     * @param x X value
     * @param y Y value
     * @param z Z value
     */
    void setModelPosition(double x, double y, double z);

    /**
     * Applies a rotation to the model
     * @param axisX X component of the rotation axis
     * @param axisY Y component of the rotation axis
     * @param axisZ Z component of the rotation axis
     * @param angleDegrees angle to rotate in degrees
     */
    void setModelRotation(double axisX, double axisY, double axisZ, double angleDegrees);

    /**
     * Returns the current 3D model type displayed
     * @return The current model type displayed
     */
    ModelType getCurrentModelType();
}
