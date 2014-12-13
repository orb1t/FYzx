/*
 * Copyright (c) 2007, 2013, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Oracle designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Oracle in the LICENSE file that accompanied this code.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact Oracle, 500 Oracle Parkway, Redwood Shores, CA 94065 USA
 * or visit www.oracle.com if you need additional information or have any
 * questions.
 */
package util;

import javafx.geometry.Point2D;
import javafx.geometry.Point3D;
import javafx.scene.transform.Affine;
import javafx.scene.transform.NonInvertibleTransformException;

/**
 * A ray used for picking.
 */
public class Ray {

    private Point3D origin = new Point3D(0, 0, 0);
    private Point3D direction = new Point3D(0, 0, 0);
    private double nearClip = 0.0;
    private double farClip = Double.POSITIVE_INFINITY;

//    static final double EPS = 1.0e-13;
    static final double EPS = 1.0e-5f;

    public Ray() {
    }

    public Ray(Point3D origin, Point3D direction, double nearClip, double farClip) {
        set(origin, direction, nearClip, farClip);
    }

    public Ray(double x, double y, double z, double nearClip, double farClip) {
        set(x, y, z, nearClip, farClip);
    }

    public static Ray computePerspectivePickRay(
            double x, double y, boolean fixedEye,
            double viewWidth, double viewHeight,
            double fieldOfViewRadians, boolean verticalFieldOfView,
            Affine transform,
            double nearClip, double farClip,
            Ray pickRay) {

        if (pickRay == null) {
            pickRay = new Ray();
        }

        Point3D direction = pickRay.getDirectionNoClone();
        double halfViewWidth = viewWidth / 2.0;
        double halfViewHeight = viewHeight / 2.0;
        double halfViewDim = verticalFieldOfView ? halfViewHeight : halfViewWidth;
        // Distance to projection plane from eye
        double distanceZ = halfViewDim / Math.tan(fieldOfViewRadians / 2.0);
        direction = Point3D.ZERO.add(x - halfViewWidth, y - halfViewHeight, distanceZ);

        Point3D eye = pickRay.getOriginNoClone();

        if (fixedEye) {
            eye = Point3D.ZERO;
        } else {
            // set eye at center of viewport and move back so that projection plane
            // is at Z = 0
            eye = new Point3D(halfViewWidth, halfViewHeight, -distanceZ);
        }

        pickRay.nearClip = nearClip * (direction.magnitude() / (fixedEye ? distanceZ : 1.0));
        pickRay.farClip = farClip * (direction.magnitude() / (fixedEye ? distanceZ : 1.0));

        pickRay.transform(transform);

        return pickRay;
    }

    public static Ray computeParallelPickRay(
            double x, double y, double viewHeight,
            Affine transform,
            double nearClip, double farClip,
            Ray pickRay) {

        if (pickRay == null) {
            pickRay = new Ray();
        }

        // This is the same math as in the perspective case, fixed
        // for the default 30 degrees vertical field of view.
        final double distanceZ = (viewHeight / 2.0)
                / Math.tan(Math.toRadians(15.0));

        pickRay.set(x, y, distanceZ, nearClip * distanceZ, farClip * distanceZ);

        if (transform != null) {
            pickRay.transform(transform);
        }

        return pickRay;
    }

    public final void set(Point3D origin, Point3D direction, double nearClip, double farClip) {
        setOrigin(origin);
        setDirection(direction);
        this.nearClip = nearClip;
        this.farClip = farClip;
    }

    public final void set(double x, double y, double z, double nearClip, double farClip) {
        setOrigin(x, y, -z);
        setDirection(0, 0, z);
        this.nearClip = nearClip;
        this.farClip = farClip;
    }

    public void setPickRay(Ray other) {
        setOrigin(other.origin);
        setDirection(other.direction);
        nearClip = other.nearClip;
        farClip = other.farClip;
    }

    public Ray copy() {
        return new Ray(origin, direction, nearClip, farClip);
    }

    /**
     * Sets the origin of the pick ray in world coordinates.
     *
     * @param origin the origin (in world coordinates).
     */
    public void setOrigin(Point3D origin) {
        this.origin = origin;
    }

    /**
     * Sets the origin of the pick ray in world coordinates.
     *
     * @param x the origin X coordinate
     * @param y the origin Y coordinate
     * @param z the origin Z coordinate
     */
    public void setOrigin(double x, double y, double z) {
        this.origin = new Point3D(x, y, z);
    }

    public Point3D getOrigin(Point3D rv) {
        rv = new Point3D(origin.getX(), origin.getY(), origin.getZ());
        return rv;
    }

    public Point3D getOriginNoClone() {
        return origin;
    }

    /**
     * Sets the direction vector of the pick ray. This vector need not be
     * normalized.
     *
     * @param direction the direction vector
     */
    public void setDirection(Point3D direction) {
        this.direction = direction;
    }

    /**
     * Sets the direction of the pick ray. The vector need not be normalized.
     *
     * @param x the direction X magnitude
     * @param y the direction Y magnitude
     * @param z the direction Z magnitude
     */
    public void setDirection(double x, double y, double z) {
        this.direction = new Point3D(x, y, z);
    }

    public Point3D getDirection(Point3D rv) {
        rv = new Point3D(direction.getX(), direction.getY(), direction.getZ());
        return rv;
    }

    public Point3D getDirectionNoClone() {
        return direction;
    }

    public double getNearClip() {
        return nearClip;
    }

    public double getFarClip() {
        return farClip;
    }

    public double distance(Point3D iPnt) {
        double x = iPnt.getX() - origin.getX();
        double y = iPnt.getY() - origin.getY();
        double z = iPnt.getZ() - origin.getZ();
        return Math.sqrt(x * x + y * y + z * z);
    }

    /**
     * Project the ray through the specified (inverted) transform and onto the
     * Z=0 plane of the resulting coordinate system. If a perspective projection
     * is being used then only a point that projects forward from the eye to the
     * plane will be returned, otherwise a null will be returned to indicate
     * that the projection is behind the eye.
     *
     * @param inversetx the inverse of the model transform into which the ray is
     * to be projected
     * @param perspective true if the projection is happening in perspective
     * @param tmpvec a temporary {@code Point3D} object for internal use (may be
     * null)
     * @param ret a {@code Point2D} object for storing the return value, or null
     * if a new object should be returned.
     * @return
     */
    public Point2D projectToZeroPlane(Affine inversetx,
            boolean perspective,
            Point3D tmpvec, Point2D ret) {
        if (tmpvec == null) {
            tmpvec = new Point3D(0,0,0);
        }
        tmpvec = inversetx.transform(origin);
        double origX = tmpvec.getX();
        double origY = tmpvec.getX();
        double origZ = tmpvec.getX();
        tmpvec = origin.add(direction);
        tmpvec = inversetx.transform(tmpvec);
        double dirX = tmpvec.getX() - origX;
        double dirY = tmpvec.getY() - origY;
        double dirZ = tmpvec.getZ() - origZ;
        // Handle the case where pickRay is almost parallel to the Z-plane
        if (almostZero(dirZ)) {
            return null;
        }
        double t = -origZ / dirZ;
        if (perspective && t < 0) {
            // TODO: Or should we use Infinity? (RT-26888)
            return null;
        }
        if (ret == null) {
            ret = Point2D.ZERO;
        }
        ret = new Point2D((float) (origX + (dirX * t)),
                (float) (origY + (dirY * t)));
        return ret;
    }

    // Good to find a home for commonly use util. code such as EPS.
    // and almostZero. This code currently defined in multiple places,
    // such as Affine3D and GeneralTransform3D.
    private static final double EPSILON_ABSOLUTE = 1.0e-5;

    static boolean almostZero(double a) {
        return ((a < EPSILON_ABSOLUTE) && (a > -EPSILON_ABSOLUTE));
    }

    private static boolean isNonZero(double v) {
        return ((v > EPS) || (v < -EPS));

    }

    public void transform(Affine t) {
        t.transform(origin);
        t.deltaTransform(direction);
    }

    public void inverseTransform(Affine t)throws NonInvertibleTransformException {
        t.inverseTransform(origin);
        t.inverseDeltaTransform(direction);
    }

    public Ray project(Affine inversetx,
            boolean perspective,
            Point3D tmpvec, Point2D ret) {
        if (tmpvec == null) {
            tmpvec = new Point3D(0, 0, 0);
        }
        tmpvec = inversetx.transform(origin);
        double origX = tmpvec.getX();
        double origY = tmpvec.getY();
        double origZ = tmpvec.getZ();
        tmpvec = tmpvec.add(direction);
        tmpvec = inversetx.transform(tmpvec);
        double dirX = tmpvec.getX() - origX;
        double dirY = tmpvec.getY() - origY;
        double dirZ = tmpvec.getZ() - origZ;

        Ray pr = new Ray();
        pr.setOrigin(origX, origY, origZ);
        pr.setDirection(dirX, dirY, dirZ);

        return pr;
    }

    @Override
    public String toString() {
        return "origin: " + origin + "  direction: " + direction;
    }
}
