/*****************************************************************
|
|    AP4 - sample entries
|
|    Copyright 2002-2008 Axiomatic Systems, LLC
|
|
|    This file is part of Bento4/AP4 (MP4 Atom Processing Library).
|
|    Unless you have obtained Bento4 under a difference license,
|    this version of Bento4 is Bento4|GPL.
|    Bento4|GPL is free software; you can redistribute it and/or modify
|    it under the terms of the GNU General Public License as published by
|    the Free Software Foundation; either version 2, or (at your option)
|    any later version.
|
|    Bento4|GPL is distributed in the hope that it will be useful,
|    but WITHOUT ANY WARRANTY; without even the implied warranty of
|    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
|    GNU General Public License for more details.
|
|    You should have received a copy of the GNU General Public License
|    along with Bento4|GPL; see the file COPYING.  If not, write to the
|    Free Software Foundation, 59 Temple Place - Suite 330, Boston, MA
|    02111-1307, USA.
|
 ****************************************************************/

#ifndef _AP4_SAMPLE_ENTRY_H_
#define _AP4_SAMPLE_ENTRY_H_

/*----------------------------------------------------------------------
|   includes
+---------------------------------------------------------------------*/
#include "Ap4Types.h"
#include "Ap4List.h"
#include "Ap4Atom.h"
#include "Ap4EsdsAtom.h"
#include "Ap4AtomFactory.h"
#include "Ap4ContainerAtom.h"

/*----------------------------------------------------------------------
|   class references
+---------------------------------------------------------------------*/
class AP4_SampleDescription;
class AP4_AvccAtom;
class AP4_HvccAtom;

/*----------------------------------------------------------------------
|   AP4_SampleEntry
+---------------------------------------------------------------------*/
class AP4_SampleEntry : public AP4_ContainerAtom
{
 public: 
    AP4_IMPLEMENT_DYNAMIC_CAST_D(AP4_SampleEntry, AP4_ContainerAtom)

    // methods
    AP4_SampleEntry(AP4_Atom::Type format);
    AP4_SampleEntry(AP4_Atom::Type   format, 
                    AP4_Size         size,
                    AP4_ByteStream&  stream,
                    AP4_AtomFactory& atom_factory);
    virtual ~AP4_SampleEntry() {}
    
    AP4_UI16           GetDataReferenceIndex() { return m_DataReferenceIndex; }
    virtual AP4_Result Write(AP4_ByteStream& stream);
    virtual AP4_Result Inspect(AP4_AtomInspector& inspector);
    virtual AP4_SampleDescription* ToSampleDescription();

    // AP4_AtomParent methods
    virtual void OnChildChanged(AP4_Atom* child);

 protected:
    // constructor
    AP4_SampleEntry(AP4_Atom::Type format, AP4_Size size);

    // methods
    virtual void       Read(AP4_ByteStream& stream, AP4_AtomFactory& atom_factory);
    virtual AP4_Size   GetFieldsSize();
    virtual AP4_Result ReadFields(AP4_ByteStream& stream);
    virtual AP4_Result WriteFields(AP4_ByteStream& stream);
    virtual AP4_Result InspectFields(AP4_AtomInspector& inspector);

    // members
    AP4_UI08 m_Reserved1[6];         // = 0
    AP4_UI16 m_DataReferenceIndex;
};

/*----------------------------------------------------------------------
|   AP4_UnknownSampleEntry
+---------------------------------------------------------------------*/
class AP4_UnknownSampleEntry : public AP4_SampleEntry
{
 public: 
    // constructors
    AP4_UnknownSampleEntry(AP4_Atom::Type type, AP4_Size size, AP4_ByteStream& stream);
    AP4_UnknownSampleEntry(AP4_Atom::Type type, AP4_DataBuffer& payload);
    
    // AP4_Atom methods
    AP4_Atom* Clone();
    
    // AP4_SampleEntry methods
    virtual AP4_SampleDescription* ToSampleDescription();

    // accessors
    const AP4_DataBuffer& GetPayload() { return m_Payload; }
    
 protected:
    // methods
    virtual AP4_Size   GetFieldsSize();
    virtual AP4_Result ReadFields(AP4_ByteStream& stream);
    virtual AP4_Result WriteFields(AP4_ByteStream& stream);

    // members
    AP4_DataBuffer m_Payload;
};

/*----------------------------------------------------------------------
|   AP4_AudioSampleEntry
+---------------------------------------------------------------------*/
class AP4_AudioSampleEntry : public AP4_SampleEntry
{
public:
    // methods
    AP4_AudioSampleEntry(AP4_Atom::Type   format,
                         AP4_UI32         sample_rate,
                         AP4_UI16         sample_size,
                         AP4_UI16         channel_count);
    AP4_AudioSampleEntry(AP4_Atom::Type   format,
                         AP4_Size         size,
                         AP4_ByteStream&  stream,
                         AP4_AtomFactory& atom_factory);

    // accessors
    AP4_UI32 GetSampleRate();
    AP4_UI16 GetSampleSize() { return m_SampleSize; }
    AP4_UI16 GetChannelCount();

    // methods
    AP4_SampleDescription* ToSampleDescription();

protected:
    // methods
    virtual AP4_Size   GetFieldsSize();
    virtual AP4_Result ReadFields(AP4_ByteStream& stream);
    virtual AP4_Result WriteFields(AP4_ByteStream& stream);
    virtual AP4_Result InspectFields(AP4_AtomInspector& inspector);

    // members
    AP4_UI16 m_QtVersion;       // 0, 1 or 2
    AP4_UI16 m_QtRevision;      // 0
    AP4_UI32 m_QtVendor;        // 0
    AP4_UI16 m_ChannelCount; 
    AP4_UI16 m_SampleSize; 
    AP4_UI16 m_QtCompressionId; // 0 or -2
    AP4_UI16 m_QtPacketSize;    // always 0
    AP4_UI32 m_SampleRate;      // 16.16 fixed point   
    
    AP4_UI32 m_QtV1SamplesPerPacket;
    AP4_UI32 m_QtV1BytesPerPacket;
    AP4_UI32 m_QtV1BytesPerFrame;
    AP4_UI32 m_QtV1BytesPerSample;
 
    AP4_UI32 m_QtV2StructSize;
    double   m_QtV2SampleRate64;
    AP4_UI32 m_QtV2ChannelCount;
    AP4_UI32 m_QtV2Reserved;
    AP4_UI32 m_QtV2BitsPerChannel;
    AP4_UI32 m_QtV2FormatSpecificFlags;
    AP4_UI32 m_QtV2BytesPerAudioPacket;
    AP4_UI32 m_QtV2LPCMFramesPerAudioPacket;
    AP4_DataBuffer m_QtV2Extension;
};

/*----------------------------------------------------------------------
|   AP4_VisualSampleEntry
+---------------------------------------------------------------------*/
class AP4_VisualSampleEntry : public AP4_SampleEntry
{
public:
    // methods
    AP4_VisualSampleEntry(AP4_Atom::Type    format, 
                          AP4_UI16          width,
                          AP4_UI16          height,
                          AP4_UI16          depth,
                          const char*       compressor_name);
    AP4_VisualSampleEntry(AP4_Atom::Type   format,
                          AP4_Size         size,
                          AP4_ByteStream&  stream,
                          AP4_AtomFactory& atom_factory);

    // accessors
    AP4_UI16    GetWidth()          { return m_Width;  }
    AP4_UI16    GetHeight()         { return m_Height; }
    AP4_UI32    GetHorizResolution(){ return m_HorizResolution;  }
    AP4_UI32    GetVertResolution() { return m_VertResolution; }
    AP4_UI16    GetDepth()          { return m_Depth;  }
    const char* GetCompressorName() { return m_CompressorName.GetChars(); }

    // methods
    AP4_SampleDescription* ToSampleDescription();

protected:
    // methods
    virtual AP4_Size   GetFieldsSize();
    virtual AP4_Result ReadFields(AP4_ByteStream& stream);
    virtual AP4_Result WriteFields(AP4_ByteStream& stream);
    virtual AP4_Result InspectFields(AP4_AtomInspector& inspector);

    //members
    AP4_UI16   m_Predefined1;     // = 0
    AP4_UI16   m_Reserved2;       // = 0
    AP4_UI08   m_Predefined2[12]; // = 0
    AP4_UI16   m_Width;
    AP4_UI16   m_Height;
    AP4_UI32   m_HorizResolution; // = 0x00480000 (72 dpi)
    AP4_UI32   m_VertResolution;  // = 0x00480000 (72 dpi)
    AP4_UI32   m_Reserved3;       // = 0
    AP4_UI16   m_FrameCount;      // = 1
    AP4_String m_CompressorName;       
    AP4_UI16   m_Depth;           // = 0x0018
    AP4_UI16   m_Predefined3;     // = 0xFFFF
};

/*----------------------------------------------------------------------
|   AP4_MpegSystemSampleEntry
+---------------------------------------------------------------------*/
class AP4_MpegSystemSampleEntry : public AP4_SampleEntry
{
public:
    // constructors
    AP4_MpegSystemSampleEntry(AP4_UI32          type,
                              AP4_EsDescriptor* descriptor);
    AP4_MpegSystemSampleEntry(AP4_UI32         type,
                              AP4_Size         size,
                              AP4_ByteStream&  stream,
                              AP4_AtomFactory& atom_factory);

    // methods
    AP4_SampleDescription* ToSampleDescription();
};

/*----------------------------------------------------------------------
|   AP4_MpegAudioSampleEntry
+---------------------------------------------------------------------*/
class AP4_MpegAudioSampleEntry : public AP4_AudioSampleEntry
{
public:
    // constructors
    AP4_MpegAudioSampleEntry(AP4_UI32          type,
                             AP4_UI32          sample_rate, 
                             AP4_UI16          sample_size,
                             AP4_UI16          channel_count,
                             AP4_EsDescriptor* descriptor);
    AP4_MpegAudioSampleEntry(AP4_UI32         type,
                             AP4_Size         size,
                             AP4_ByteStream&  stream,
                             AP4_AtomFactory& atom_factory);

    // methods
    AP4_SampleDescription* ToSampleDescription();
};

/*----------------------------------------------------------------------
|   AP4_MpegVideoSampleEntry
+---------------------------------------------------------------------*/
class AP4_MpegVideoSampleEntry : public AP4_VisualSampleEntry
{
public:
    // constructors
    AP4_MpegVideoSampleEntry(AP4_UI32          type,
                             AP4_UI16          width,
                             AP4_UI16          height,
                             AP4_UI16          depth,
                             const char*       compressor_name,
                             AP4_EsDescriptor* descriptor);
    AP4_MpegVideoSampleEntry(AP4_UI32         type,
                             AP4_Size         size,
                             AP4_ByteStream&  stream,
                             AP4_AtomFactory& atom_factory);

    // methods
    AP4_SampleDescription* ToSampleDescription();
};

/*----------------------------------------------------------------------
|   AP4_Mp4sSampleEntry
+---------------------------------------------------------------------*/
class AP4_Mp4sSampleEntry : public AP4_MpegSystemSampleEntry
{
 public:
    // constructors
    AP4_Mp4sSampleEntry(AP4_Size         size,
                        AP4_ByteStream&  stream,
                        AP4_AtomFactory& atom_factory);
    AP4_Mp4sSampleEntry(AP4_EsDescriptor* descriptor);

    // methods
    AP4_SampleDescription* ToSampleDescription();
};

/*----------------------------------------------------------------------
|   AP4_Mp4aSampleEntry
+---------------------------------------------------------------------*/
class AP4_Mp4aSampleEntry : public AP4_MpegAudioSampleEntry
{
 public:
    // constructors
    AP4_Mp4aSampleEntry(AP4_Size         size,
                        AP4_ByteStream&  stream,
                        AP4_AtomFactory& atom_factory);
    AP4_Mp4aSampleEntry(AP4_UI32          sample_rate, 
                        AP4_UI16          sample_size,
                        AP4_UI16          channel_count,
                        AP4_EsDescriptor* descriptor);
};

/*----------------------------------------------------------------------
|   AP4_Mp4vSampleEntry
+---------------------------------------------------------------------*/
class AP4_Mp4vSampleEntry : public AP4_MpegVideoSampleEntry
{
 public:
    // constructors
    AP4_Mp4vSampleEntry(AP4_Size         size,
                        AP4_ByteStream&  stream,
                        AP4_AtomFactory& atom_factory);
    AP4_Mp4vSampleEntry(AP4_UI16          width,
                        AP4_UI16          height,
                        AP4_UI16          depth,
                        const char*       compressor_name,
                        AP4_EsDescriptor* descriptor);                        
};

/*----------------------------------------------------------------------
|   AP4_AvcSampleEntry
+---------------------------------------------------------------------*/
class AP4_AvcSampleEntry : public AP4_VisualSampleEntry
{
public:
    // constructors
    AP4_AvcSampleEntry(AP4_UI32         format, // avc1, avc2, avc3, avc4
                       AP4_Size         size,
                       AP4_ByteStream&  stream,
                       AP4_AtomFactory& atom_factory);
    AP4_AvcSampleEntry(AP4_UI32            format, // avc1, avc2, avc3, avc4
                       AP4_UI16            width,
                       AP4_UI16            height,
                       AP4_UI16            depth,
                       const char*         compressor_name,
                       const AP4_AvccAtom& avcc);
                        
    // inherited from AP4_SampleEntry
    virtual AP4_SampleDescription* ToSampleDescription();
};

/*----------------------------------------------------------------------
|   AP4_HevcSampleEntry
+---------------------------------------------------------------------*/
class AP4_HevcSampleEntry : public AP4_VisualSampleEntry
{
public:
    // constructors
    AP4_HevcSampleEntry(AP4_UI32         format, // hvc1, hev1
                        AP4_Size         size,
                        AP4_ByteStream&  stream,
                        AP4_AtomFactory& atom_factory);
    
    AP4_HevcSampleEntry(AP4_UI32            format, // hvc1, hev1
                        AP4_UI16            width,
                        AP4_UI16            height,
                        AP4_UI16            depth,
                        const char*         compressor_name,
                        const AP4_HvccAtom& hvcc);

    // inherited from AP4_SampleEntry
    virtual AP4_SampleDescription* ToSampleDescription();
};

/*----------------------------------------------------------------------
|   AP4_RtpHintSampleEntry
+---------------------------------------------------------------------*/
class AP4_RtpHintSampleEntry : public AP4_SampleEntry
{
public:
    // methods
    AP4_RtpHintSampleEntry(AP4_UI16 hint_track_version,
                           AP4_UI16 highest_compatible_version,
                           AP4_UI32 max_packet_size,
                           AP4_UI32 timescale);
    AP4_RtpHintSampleEntry(AP4_Size         size,
                           AP4_ByteStream&  stream,
                           AP4_AtomFactory& atom_factory);
    
protected:
    // methods
    virtual AP4_Size   GetFieldsSize();
    virtual AP4_Result ReadFields(AP4_ByteStream& stream);
    virtual AP4_Result WriteFields(AP4_ByteStream& stream);
    virtual AP4_Result InspectFields(AP4_AtomInspector& inspector);

    // members
    AP4_UI16 m_HintTrackVersion;
    AP4_UI16 m_HighestCompatibleVersion;
    AP4_UI32 m_MaxPacketSize;
};

/*----------------------------------------------------------------------
|   AP4_SubtitleSampleEntry
+---------------------------------------------------------------------*/
class AP4_SubtitleSampleEntry : public AP4_SampleEntry
{
public:
    // methods
    AP4_SubtitleSampleEntry(AP4_Atom::Type format,
                            const char* namespce,
                            const char* schema_location,
                            const char* image_mime_type);
    AP4_SubtitleSampleEntry(AP4_Atom::Type   format,
                            AP4_Size         size,
                            AP4_ByteStream&  stream,
                            AP4_AtomFactory& atom_factory);

    // accessors

    // methods
    AP4_SampleDescription* ToSampleDescription();

protected:
    // methods
    virtual AP4_Size   GetFieldsSize();
    virtual AP4_Result ReadFields(AP4_ByteStream& stream);
    virtual AP4_Result WriteFields(AP4_ByteStream& stream);
    virtual AP4_Result InspectFields(AP4_AtomInspector& inspector);

    // members
    AP4_String m_Namespace;
    AP4_String m_SchemaLocation;
    AP4_String m_ImageMimeType;
};

#endif // _AP4_SAMPLE_ENTRY_H_
