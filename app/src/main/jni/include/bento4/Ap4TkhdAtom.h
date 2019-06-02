/*****************************************************************
|
|    AP4 - tkhd Atoms 
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

#ifndef _AP4_TKHD_ATOM_H_
#define _AP4_TKHD_ATOM_H_

/*----------------------------------------------------------------------
|   includes
+---------------------------------------------------------------------*/
#include "Ap4Atom.h"
#include "Ap4Utils.h"

/*----------------------------------------------------------------------
|   constants
+---------------------------------------------------------------------*/
const int AP4_TKHD_FLAG_TRACK_ENABLED    = 1;
const int AP4_TKHD_FLAG_TRACK_IN_MOVIE   = 2;
const int AP4_TKHD_FLAG_TRACK_IN_PREVIEW = 4;

const int AP4_TKHD_FLAG_DEFAULTS         = 7;

/*----------------------------------------------------------------------
|   AP4_TkhdAtom
+---------------------------------------------------------------------*/
class AP4_TkhdAtom : public AP4_Atom
{
public:
    AP4_IMPLEMENT_DYNAMIC_CAST_D(AP4_TkhdAtom, AP4_Atom)

    // class methods
    static AP4_TkhdAtom* Create(AP4_Size size, AP4_ByteStream& stream);

    // methods
    AP4_TkhdAtom(AP4_UI32        creation_time,
                 AP4_UI32        modification_time,
                 AP4_UI32        track_id,
                 AP4_UI64        duration,
                 AP4_UI16        volume,
                 AP4_UI32        width,
                 AP4_UI32        height,
                 AP4_UI16        layer = 0,
                 AP4_UI16        alternate_group = 0,
                 const AP4_SI32* matrix = NULL);
    virtual AP4_Result InspectFields(AP4_AtomInspector& inspector);
    virtual AP4_Result WriteFields(AP4_ByteStream& stream);

    AP4_UI64 GetCreationTime() const                         { return m_CreationTime;                  }
    void     SetCreationTime(AP4_UI64 creation_time)         { m_CreationTime = creation_time;         }
    AP4_UI64 GetModificationTime() const                     { return m_ModificationTime;              }
    void     SetModificationTime(AP4_UI64 modification_time) { m_ModificationTime = modification_time; }
    AP4_UI32 GetTrackId() const                              { return m_TrackId;                       }
    void     SetTrackId(AP4_UI32 track_id)                   { m_TrackId = track_id;                   }
    AP4_UI64 GetDuration() const                             { return m_Duration;                      }
    void     SetDuration(AP4_UI64 duration)                  { m_Duration = duration;                  }
    AP4_UI16 GetLayer() const                                { return m_Layer;                         }
    void     SetLayer(AP4_UI16 layer)                        { m_Layer = layer;                        }
    AP4_UI16 GetAlternateGroup() const                       { return m_AlternateGroup;                }
    void     SetAlternateGroup(AP4_UI16 group)               { m_AlternateGroup = group;               }
    AP4_UI16 GetVolume() const                               { return m_Volume;                        }
    void     SetVolume(AP4_UI16 volume)                      { m_Volume = volume;                      }
    AP4_UI32 GetWidth() const                                { return m_Width;                         }
    void     SetWidth(AP4_UI32 width)                        { m_Width = width;                        }
    AP4_UI32 GetHeight() const                               { return m_Height;                        }
    void     SetHeight(AP4_UI32 height)                      { m_Height = height;                      }
    const AP4_SI32* GetMatrix() const                        { return m_Matrix;                        }
    void            SetMatrix(const AP4_SI32* matrix)        { AP4_CopyMemory(m_Matrix, matrix, 9*sizeof(m_Matrix[0])); }
    void GetTranslation(float& x, float& y) {
        x = (float)(*(int*)&m_Matrix[6]) / 65536;
        y = (float)(*(int*)&m_Matrix[7]) / 65536;
    }

 private:
    // methods
    AP4_TkhdAtom(AP4_UI32        size, 
                 AP4_UI08        version,
                 AP4_UI32        flags,
                 AP4_ByteStream& stream);

    // members
    AP4_UI64 m_CreationTime;
    AP4_UI64 m_ModificationTime;
    AP4_UI32 m_TrackId;
    AP4_UI32 m_Reserved1;
    AP4_UI64 m_Duration;
    AP4_UI32 m_Reserved2[2];
    AP4_UI16 m_Layer;
    AP4_UI16 m_AlternateGroup;
    AP4_UI16 m_Volume;
    AP4_UI16 m_Reserved3;
    AP4_SI32 m_Matrix[9];
    AP4_UI32 m_Width;
    AP4_UI32 m_Height;
};

#endif // _AP4_TKHD_ATOM_H_
