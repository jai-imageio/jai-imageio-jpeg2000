/*
 * $RCSfile: ROI.java,v $
 * $Revision: 1.1 $
 * $Date: 2005/02/11 05:02:22 $
 * $State: Exp $
 *
 * Class:                   ROI
 *
 * Description:             This class describes a single ROI
 *
 *
 *
 * COPYRIGHT:
 *
 * This software module was originally developed by Raphaël Grosbois and
 * Diego Santa Cruz (Swiss Federal Institute of Technology-EPFL); Joel
 * Askelöf (Ericsson Radio Systems AB); and Bertrand Berthelot, David
 * Bouchard, Félix Henry, Gerard Mozelle and Patrice Onno (Canon Research
 * Centre France S.A) in the course of development of the JPEG2000
 * standard as specified by ISO/IEC 15444 (JPEG 2000 Standard). This
 * software module is an implementation of a part of the JPEG 2000
 * Standard. Swiss Federal Institute of Technology-EPFL, Ericsson Radio
 * Systems AB and Canon Research Centre France S.A (collectively JJ2000
 * Partners) agree not to assert against ISO/IEC and users of the JPEG
 * 2000 Standard (Users) any of their rights under the copyright, not
 * including other intellectual property rights, for this software module
 * with respect to the usage by ISO/IEC and Users of this software module
 * or modifications thereof for use in hardware or software products
 * claiming conformance to the JPEG 2000 Standard. Those intending to use
 * this software module in hardware or software products are advised that
 * their use may infringe existing patents. The original developers of
 * this software module, JJ2000 Partners and ISO/IEC assume no liability
 * for use of this software module or modifications thereof. No license
 * or right to this software module is granted for non JPEG 2000 Standard
 * conforming products. JJ2000 Partners have full right to use this
 * software module for his/her own purpose, assign or donate this
 * software module to any third party and to inhibit third parties from
 * using this software module for non JPEG 2000 Standard conforming
 * products. This copyright notice must be included in all copies or
 * derivative works of this software module.
 *
 * Copyright (c) 1999/2000 JJ2000 Partners.
 */
package jj2000.j2k.roi.encoder;

import jj2000.j2k.image.input.ImgReaderPGM;

/**
 * This class contains the shape of a single ROI. In the current
 * implementation only rectangles and circles are supported.
 *
 * @see ROIMaskGenerator
 */
public class ROI{

    /** ImgReaderPGM object with the arbrtrary ROI */
    public ImgReaderPGM maskPGM = null;

    /** Where or not the ROI shape is arbitrary */
    public boolean arbShape;

    /** Flag indicating whether the ROI is rectangular or not */
    public boolean rect;

    /** The components for which the ROI is relevant */
    public int comp;

    /** x coordinate of upper left corner of rectangular ROI */
    public int ulx;

    /** y coordinate of upper left corner of rectangular ROI */
    public int uly;

    /** width of rectangular ROI  */
    public int w;

    /** height of rectangular ROI */
    public int h;

    /** x coordinate of center of circular ROI */
    public int x;

    /** y coordinate of center of circular ROI */
    public int y;

    /** radius of circular ROI  */
    public int r;


    /**
     * Constructor for ROI with arbitrary shape
     *
     * @param comp The component the ROI belongs to
     *
     * @param maskPGM ImgReaderPGM containing the ROI
     */
    public ROI(int comp, ImgReaderPGM maskPGM){
        arbShape = true;
        rect = false;
        this.comp = comp;
        this.maskPGM = maskPGM;
    }

    /**
     * Constructor for rectangular ROIs
     *
     * @param comp The component the ROI belongs to
     *
     * @param ulx x-coordinate of upper left corner of ROI
     *
     * @param uly y-coordinate of upper left corner of ROI
     *
     * @param w width of ROI
     *
     * @param h height of ROI
     */
    public ROI(int comp, int ulx, int uly, int w, int h){
        arbShape = false;
        this.comp = comp;
	this.ulx = ulx;
	this.uly = uly;
	this.w = w;
	this.h = h;
	rect = true;
    }

    /**
     * Constructor for circular ROIs
     *
     * @param comp The component the ROI belongs to
     *
     * @param x x-coordinate of center of ROI
     *
     * @param y y-coordinate of center of ROI
     *
     * @param rad radius of ROI
     */
    public ROI(int comp, int x, int y, int rad){
        arbShape = false;
        this.comp = comp;
	this.x = x;
	this.y = y;
	this.r = rad;
    }

    /**
     * This function prints all relevant data for the ROI
     */
    public String toString(){
        if(arbShape) {
            return "ROI with arbitrary shape, PGM file= "+maskPGM;
        }
        else if(rect)
            return "Rectangular ROI, comp="+comp+" ulx="+ulx+" uly="+uly+
                   " w="+w+" h="+h;
        else
            return "Circular ROI,  comp="+comp+" x="+x+" y="+y+
                " radius="+r;

    }

}









