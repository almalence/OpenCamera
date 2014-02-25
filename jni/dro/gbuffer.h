/*
 * This proprietary software may be used only as
 * authorised by a licensing agreement from ARM Limited
 * (C) COPYRIGHT 2013 ARM Limited
 * ALL RIGHTS RESERVED
 * The entire notice above must be reproduced on all authorised
 * copies and copies may only be made to the extent permitted
 * by a licensing agreement from ARM Limited.
 */
#ifndef GBUFFER_H
#define GBUFFER_H

#include <stdio.h>

#include <EGL/egl.h>

class gbuffer
    {
public:

    // ----- Types -----

    enum Format
        {
        FORMAT_RGBA_8888,
        FORMAT_RGBX_8888,
        FORMAT_RGB_888,
        FORMAT_RGB_565,
        FORMAT_BGRA_8888,
        FORMAT_RGBA_5551,
        FORMAT_RGBA_4444,
        FORMAT_YV12,
        ///
        FORMATS_COUNT
        };

    // ----- A factory method of a native buffer -----

    /**
     * @brief The method creates the gbuffer with a specified size. The size in the end will represent
     *        size of a texture to which the buffer will be assigned.
     */
    static gbuffer* obtainBuffer(unsigned int aWidth, unsigned int aHeight, Format aFormat, float* mtx);
    static void putBuffer(gbuffer* buffer);

    // ----- Miscellaneous -----

    ///
    EGLClientBuffer getNativeBuffer();

    int getWidth();
    int getHeight();

    /**
     * @brief The method locks the buffer for writing and returns a pointer, which is used for updatind data in of the texture
     *        being assigned to this buffer.
     */
    void* lock();

    /**
     * @brief Unlock buffer. After calling this method, the pointer returned from the "lock" function is forbidden for further usage.
     */
    void unlock();

private:

    // ----- Fields -----

    //
    void* theNativeAndroidBuffer;
    //
    unsigned int theWidth;
    unsigned int theHeight;
    Format theFormat;

    // ----- Constructors and destructors -----

    //
    gbuffer(unsigned int aWidth, unsigned int aHeight, Format aFormat);

    //
    virtual ~gbuffer();

    // ----- Miscellaneous internal only -----

    /**
     * @brief The method creates and initializes the native Android buffer
     */
    bool init();

    // ----- Forbidden operations on the object -----

    //
    gbuffer();
    gbuffer(gbuffer& aOther);
    gbuffer& operator=(const gbuffer& aOther);
    bool operator==(const gbuffer& aOther) const;
    bool operator!=(const gbuffer& aOther) const;
    bool operator>(const gbuffer& aOther) const;
    bool operator>=(const gbuffer& aOther) const;
    bool operator<=(const gbuffer& aOther) const;
    };

#endif
