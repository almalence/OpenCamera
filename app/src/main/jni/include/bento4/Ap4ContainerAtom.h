/*****************************************************************
|
|    AP4 - Container Atoms
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

#ifndef _AP4_CONTAINER_ATOM_H_
#define _AP4_CONTAINER_ATOM_H_

/*----------------------------------------------------------------------
|   includes
+---------------------------------------------------------------------*/
#include "Ap4Types.h"
#include "Ap4List.h"
#include "Ap4Atom.h"

/*----------------------------------------------------------------------
|   class references
+---------------------------------------------------------------------*/
class AP4_ByteStream;
class AP4_AtomFactory;

/*----------------------------------------------------------------------
|   AP4_ContainerAtom
+---------------------------------------------------------------------*/
class AP4_ContainerAtom : public AP4_Atom, public AP4_AtomParent 
{
public:
    AP4_IMPLEMENT_DYNAMIC_CAST_D2(AP4_ContainerAtom, AP4_Atom, AP4_AtomParent)

    // class methods
    static AP4_ContainerAtom* Create(Type             type, 
                                     AP4_UI64         size, 
                                     bool             is_full,
                                     bool             force_64,
                                     AP4_ByteStream&  stream,
                                     AP4_AtomFactory& atom_factory);

    // methods
    explicit AP4_ContainerAtom(Type type);
    explicit AP4_ContainerAtom(Type type, AP4_UI08 version, AP4_UI32 flags); 
    explicit AP4_ContainerAtom(Type type, AP4_UI64 size, bool force_64);
    explicit AP4_ContainerAtom(Type type, AP4_UI64 size, bool force_64, AP4_UI08 version, AP4_UI32 flags);
    AP4_List<AP4_Atom>& GetChildren() { return m_Children; }
    virtual AP4_Result InspectFields(AP4_AtomInspector& inspector);
    virtual AP4_Result InspectChildren(AP4_AtomInspector& inspector);
    virtual AP4_Result WriteFields(AP4_ByteStream& stream);
    virtual AP4_Atom*  Clone();

    // AP4_AtomParent methods
    virtual void OnChildChanged(AP4_Atom* child);
    virtual void OnChildAdded(AP4_Atom* child);
    virtual void OnChildRemoved(AP4_Atom* child);

protected:
    // constructors
    AP4_ContainerAtom(Type             type, 
                      AP4_UI64         size, 
                      bool             force_64,
                      AP4_UI08         version,
                      AP4_UI32         flags,
                      AP4_ByteStream&  stream,
                      AP4_AtomFactory& atom_factory);
    AP4_ContainerAtom(Type             type, 
                      AP4_UI64         size, 
                      bool             force_64,
                      AP4_ByteStream&  stream,
                      AP4_AtomFactory& atom_factory);

    // methods
    void ReadChildren(AP4_AtomFactory& atom_factory,
                      AP4_ByteStream&  stream, 
                      AP4_UI64         size);
};

#endif // _AP4_CONTAINER_ATOM_H_
