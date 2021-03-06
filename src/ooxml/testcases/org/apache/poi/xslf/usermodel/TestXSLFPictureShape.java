/* ====================================================================
   Licensed to the Apache Software Foundation (ASF) under one or more
   contributor license agreements.  See the NOTICE file distributed with
   this work for additional information regarding copyright ownership.
   The ASF licenses this file to You under the Apache License, Version 2.0
   (the "License"); you may not use this file except in compliance with
   the License.  You may obtain a copy of the License at

       http://www.apache.org/licenses/LICENSE-2.0

   Unless required by applicable law or agreed to in writing, software
   distributed under the License is distributed on an "AS IS" BASIS,
   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
   See the License for the specific language governing permissions and
   limitations under the License.
==================================================================== */
package org.apache.poi.xslf.usermodel;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.poi.POIDataSamples;
import org.apache.poi.sl.usermodel.PictureData.PictureType;
import org.apache.poi.util.IOUtils;
import org.apache.poi.xslf.XSLFTestDataSamples;
import org.junit.Test;
import org.openxmlformats.schemas.presentationml.x2006.main.CTPicture;

public class TestXSLFPictureShape {
    private static POIDataSamples _slTests = POIDataSamples.getSlideShowInstance();

    @Test
    public void testCreate() throws Exception {
        XMLSlideShow ppt1 = new XMLSlideShow();
        assertEquals(0, ppt1.getPictureData().size());
        byte[] data1 = new byte[100];
        for(int i = 0;i < 100;i++) { data1[i] = (byte)i; }
        XSLFPictureData pdata1 = ppt1.addPicture(data1, PictureType.JPEG);
        assertEquals(0, pdata1.getIndex());
        assertEquals(1, ppt1.getPictureData().size());

        XSLFSlide slide = ppt1.createSlide();
        XSLFPictureShape shape1 = slide.createPicture(pdata1);
        assertNotNull(shape1.getPictureData());
        assertArrayEquals(data1, shape1.getPictureData().getData());

        byte[] data2 = new byte[200];
        for(int i = 0;i < 200;i++) { data2[i] = (byte)i; }
        XSLFPictureData pdata2 = ppt1.addPicture(data2, PictureType.PNG);
        XSLFPictureShape shape2 = slide.createPicture(pdata2);
        assertNotNull(shape2.getPictureData());
        assertEquals(1, pdata2.getIndex());
        assertEquals(2, ppt1.getPictureData().size());
        assertArrayEquals(data2, shape2.getPictureData().getData());

        XMLSlideShow ppt2 = XSLFTestDataSamples.writeOutAndReadBack(ppt1);
        ppt1.close();
        List<XSLFPictureData> pics = ppt2.getPictureData();
        assertEquals(2, pics.size());
        assertArrayEquals(data1, pics.get(0).getData());
        assertArrayEquals(data2, pics.get(1).getData());

        List<XSLFShape> shapes = ppt2.getSlides().get(0).getShapes();
        assertArrayEquals(data1, ((XSLFPictureShape) shapes.get(0)).getPictureData().getData());
        assertArrayEquals(data2, ((XSLFPictureShape) shapes.get(1)).getPictureData().getData());
        ppt2.close();
    }

    @Test
    public void testCreateMultiplePictures() throws Exception {
        XMLSlideShow ppt1 = new XMLSlideShow();
        XSLFSlide slide1 = ppt1.createSlide();
        XSLFGroupShape group1 = slide1.createGroup();


        int pictureIndex = 0;
        // first add 20 images to the slide
        for (int i = 0; i < 20; i++, pictureIndex++) {
            byte[] data = new byte[]{(byte)pictureIndex};
            XSLFPictureData elementData = ppt1.addPicture(data, PictureType.PNG);
            assertEquals(pictureIndex, elementData.getIndex());   // added images have indexes 0,1,2....19
            XSLFPictureShape picture = slide1.createPicture(elementData);
            // POI saves images as image1.png, image2.png, etc.
            String fileName = "image" + (elementData.getIndex()+1) + ".png";
            assertEquals(fileName, picture.getPictureData().getFileName());
            assertArrayEquals(data, picture.getPictureData().getData());
        }

        // and then add next 20 images to a group
        for (int i = 0; i < 20; i++, pictureIndex++) {
            byte[] data = new byte[]{(byte)pictureIndex};
            XSLFPictureData elementData = ppt1.addPicture(data, PictureType.PNG);
            XSLFPictureShape picture = group1.createPicture(elementData);
            // POI saves images as image1.png, image2.png, etc.
            assertEquals(pictureIndex, elementData.getIndex());   // added images have indexes 0,1,2....19
            String fileName = "image" + (pictureIndex + 1) + ".png";
            assertEquals(fileName, picture.getPictureData().getFileName());
            assertArrayEquals(data, picture.getPictureData().getData());
        }

        // serialize, read back and check that all images are there

        XMLSlideShow ppt2 = XSLFTestDataSamples.writeOutAndReadBack(ppt1);
        ppt1.close();
        // pictures keyed by file name
        Map<String, XSLFPictureData> pics = new HashMap<>();
        for(XSLFPictureData p : ppt2.getPictureData()){
            pics.put(p.getFileName(), p);
        }
        assertEquals(40, pics.size());
        for (int i = 0; i < 40; i++) {
            byte[] data1 = new byte[]{(byte)i};
            String fileName = "image" + (i + 1) + ".png";
            XSLFPictureData data = pics.get(fileName);
            assertNotNull(data);
            assertEquals(fileName, data.getFileName());
            assertArrayEquals(data1, data.getData());
        }
        ppt2.close();
    }

    @Test
    public void testImageCaching() throws Exception {
        XMLSlideShow ppt = new XMLSlideShow();
        byte[] img1 = new byte[]{1,2,3};
        byte[] img2 = new byte[]{3,4,5};
        XSLFPictureData pdata1 = ppt.addPicture(img1, PictureType.PNG);
        assertEquals(0, pdata1.getIndex());
        assertEquals(0, ppt.addPicture(img1, PictureType.PNG).getIndex());

        XSLFPictureData idx2 = ppt.addPicture(img2, PictureType.PNG);
        assertEquals(1, idx2.getIndex());
        assertEquals(1, ppt.addPicture(img2, PictureType.PNG).getIndex());

        XSLFSlide slide1 = ppt.createSlide();
        assertNotNull(slide1);
        XSLFSlide slide2 = ppt.createSlide();
        assertNotNull(slide2);

        ppt.close();
    }

    @Test
    public void testMerge() throws Exception {
        XMLSlideShow ppt1 = new XMLSlideShow();
        byte[] data1 = new byte[100];
        XSLFPictureData pdata1 = ppt1.addPicture(data1, PictureType.JPEG);

        XSLFSlide slide1 = ppt1.createSlide();
        XSLFPictureShape shape1 = slide1.createPicture(pdata1);
        CTPicture ctPic1 = (CTPicture)shape1.getXmlObject();
        ctPic1.getNvPicPr().getNvPr().addNewCustDataLst().addNewTags().setId("rId99");
        
        XMLSlideShow ppt2 = new XMLSlideShow();

        XSLFSlide slide2 = ppt2.createSlide().importContent(slide1);
        XSLFPictureShape shape2 = (XSLFPictureShape)slide2.getShapes().get(0);

        assertArrayEquals(data1, shape2.getPictureData().getData());

        CTPicture ctPic2 = (CTPicture)shape2.getXmlObject();
        assertFalse(ctPic2.getNvPicPr().getNvPr().isSetCustDataLst());

        ppt1.close();
        ppt2.close();
    }
    
    @Test
    public void bug58663() throws IOException {
        InputStream is = _slTests.openResourceAsStream("shapes.pptx");
        XMLSlideShow ppt = new XMLSlideShow(is);
        is.close();
        
        XSLFSlide slide = ppt.getSlides().get(0);
        XSLFPictureShape ps = (XSLFPictureShape)slide.getShapes().get(3);
        slide.removeShape(ps);
        
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        ppt.write(bos);
        ppt.close();
        
        XMLSlideShow ppt2 = new XMLSlideShow(new ByteArrayInputStream(bos.toByteArray()));
        assertTrue(ppt2.getPictureData().isEmpty());
        ppt2.close();
    }
    
    @Test
    public void testTiffImageBug59742() throws Exception {
        XMLSlideShow slideShow = new XMLSlideShow();
        final InputStream tiffStream = _slTests.openResourceAsStream("testtiff.tif");
        final byte[] pictureData = IOUtils.toByteArray(tiffStream);
        IOUtils.closeQuietly(tiffStream);
        
        XSLFPictureData pic = slideShow.addPicture(pictureData, PictureType.TIFF);
        assertEquals("image/tiff", pic.getContentType());
        assertEquals("image1.tiff", pic.getFileName());
        
        slideShow.close();
    }
}