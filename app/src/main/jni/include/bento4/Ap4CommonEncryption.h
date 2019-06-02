/*****************************************************************
|
|    AP4 - Common Encryption support
|
|    Copyright 2002-2011 Axiomatic Systems, LLC
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

#ifndef _AP4_COMMON_ENCRYPTION_H_
#define _AP4_COMMON_ENCRYPTION_H_

/*----------------------------------------------------------------------
|   includes
+---------------------------------------------------------------------*/
#include "Ap4Atom.h"
#include "Ap4ByteStream.h"
#include "Ap4Utils.h"
#include "Ap4Processor.h"
#include "Ap4Protection.h"
#include "Ap4PsshAtom.h"

/*----------------------------------------------------------------------
|   class declarations
+---------------------------------------------------------------------*/
class AP4_StreamCipher;
class AP4_SaizAtom;
class AP4_SaioAtom;
class AP4_CencSampleInfoTable;

/*----------------------------------------------------------------------
|   constants
+---------------------------------------------------------------------*/
const AP4_UI32 AP4_PROTECTION_SCHEME_TYPE_CENC = AP4_ATOM_TYPE('c','e','n','c');
const AP4_UI32 AP4_PROTECTION_SCHEME_VERSION_CENC_10 = 0x00010000;

const AP4_UI32 AP4_CENC_ALGORITHM_ID_NONE = 0; 
const AP4_UI32 AP4_CENC_ALGORITHM_ID_CTR  = 1; 
const AP4_UI32 AP4_CENC_ALGORITHM_ID_CBC  = 2; 

const AP4_UI32 AP4_CENC_SAMPLE_ENCRYPTION_FLAG_OVERRIDE_TRACK_ENCRYPTION_DEFAULTS = 1;
const AP4_UI32 AP4_CENC_SAMPLE_ENCRYPTION_FLAG_USE_SUB_SAMPLE_ENCRYPTION          = 2;

typedef enum {
    AP4_CENC_VARIANT_PIFF_CTR,
    AP4_CENC_VARIANT_PIFF_CBC,
    AP4_CENC_VARIANT_MPEG
} AP4_CencVariant;

/*----------------------------------------------------------------------
|   AP4_CencTrackEncryption
+---------------------------------------------------------------------*/
class AP4_CencTrackEncryption {
public:
    AP4_IMPLEMENT_DYNAMIC_CAST(AP4_CencTrackEncryption)
    
    virtual ~AP4_CencTrackEncryption() {}
    
    // methods
    AP4_Result DoInspectFields(AP4_AtomInspector& inspector);
    AP4_Result DoWriteFields(AP4_ByteStream& stream);
    
    // accessors
    AP4_UI32        GetDefaultAlgorithmId() { return m_DefaultAlgorithmId; }
    AP4_UI08        GetDefaultIvSize()      { return m_DefaultIvSize;      }
    const AP4_UI08* GetDefaultKid()         { return m_DefaultKid;         }
    
protected:
    // constructors
    AP4_CencTrackEncryption();
    AP4_CencTrackEncryption(AP4_ByteStream& stream);
    AP4_CencTrackEncryption(AP4_UI32        default_algorithm_id,
                            AP4_UI08        default_iv_size,
                            const AP4_UI08* default_kid);
    
private:
    // members
    AP4_UI32 m_DefaultAlgorithmId;
    AP4_UI08 m_DefaultIvSize;
    AP4_UI08 m_DefaultKid[16];    
};

/*----------------------------------------------------------------------
|   AP4_CencSampleEncryption
+---------------------------------------------------------------------*/
class AP4_CencSampleEncryption {
public:
    AP4_IMPLEMENT_DYNAMIC_CAST(AP4_CencSampleEncryption)

    virtual ~AP4_CencSampleEncryption() {}

    // methods
    AP4_Result DoInspectFields(AP4_AtomInspector& inspector);
    AP4_Result DoWriteFields(AP4_ByteStream& stream);
    
    // accessors
    AP4_Atom&       GetOuter()              { return m_Outer;           }
    AP4_UI32        GetAlgorithmId()        { return m_AlgorithmId;     }
    AP4_UI08        GetIvSize()             { return m_IvSize;          }
    AP4_Result      SetIvSize(AP4_UI08 iv_size);
    const AP4_UI08* GetKid()                { return m_Kid;             }
    AP4_Cardinal    GetSampleInfoCount()    { return m_SampleInfoCount; }
    AP4_Result      AddSampleInfo(const AP4_UI08* iv, AP4_DataBuffer& subsample_info);
    AP4_Result      SetSampleInfosSize(AP4_Size size);
    AP4_Result      CreateSampleInfoTable(AP4_Size                  default_iv_size,
                                          AP4_CencSampleInfoTable*& table);
    
protected:
    // constructors
    AP4_CencSampleEncryption(AP4_Atom& outer, AP4_UI08 iv_size);
    AP4_CencSampleEncryption(AP4_Atom& outer, AP4_Size size, AP4_ByteStream& stream);
    AP4_CencSampleEncryption(AP4_Atom&       outer,
                             AP4_UI32        algorithm_id,
                             AP4_UI08        iv_size,
                             const AP4_UI08* kid);
    
protected:
    // members
    AP4_Atom&      m_Outer;
    AP4_UI32       m_AlgorithmId;
    AP4_UI08       m_IvSize;
    AP4_UI08       m_Kid[16];    
    AP4_Cardinal   m_SampleInfoCount;
    AP4_DataBuffer m_SampleInfos;
    unsigned int   m_SampleInfoCursor;
};

/*----------------------------------------------------------------------
|   AP4_CencSampleInfoTable
+---------------------------------------------------------------------*/
class AP4_CencSampleInfoTable {
public:
    // class methods
    static AP4_Result Create(AP4_ProtectedSampleDescription* sample_description,
                             AP4_ContainerAtom*              traf,
                             AP4_UI32&                       algorithm_id,
                             AP4_ByteStream&                 aux_info_data,
                             AP4_Position                    aux_info_data_offset,
                             AP4_CencSampleInfoTable*&       sample_info_table);
                             
    static AP4_Result Create(AP4_ProtectedSampleDescription* sample_description,
                             AP4_ContainerAtom*              traf,
                             AP4_SaioAtom*&                  saio,
                             AP4_SaizAtom*&                  saiz,
                             AP4_CencSampleEncryption*&      sample_encryption_atom,
                             AP4_UI32&                       algorithm_id,
                             AP4_ByteStream&                 aux_info_data,
                             AP4_Position                    aux_info_data_offset,
                             AP4_CencSampleInfoTable*&       sample_info_table);
                             
    static AP4_Result Create(unsigned int              iv_size, 
                             AP4_ContainerAtom&        traf,
                             AP4_SaioAtom&             saio, 
                             AP4_SaizAtom&             saiz,
                             AP4_ByteStream&           aux_info_data,
                             AP4_Position              aux_info_data_offset, 
                             AP4_CencSampleInfoTable*& sample_info_table);
                                                          
                                                          
    // see note below regarding the serialization format
    static AP4_Result Create(const AP4_UI08*           serialized,
                             unsigned int              serialized_size,
                             AP4_CencSampleInfoTable*& sample_info_table);
    
    // constructor
    AP4_CencSampleInfoTable(AP4_UI32 sample_count,
                            AP4_UI08 iv_size);
    
    // methods
    AP4_UI32        GetSampleCount() { return m_SampleCount; }
    AP4_UI08        GetIvSize()      { return m_IvSize;      }
    AP4_Result      SetIv(AP4_Ordinal sample_index, const AP4_UI08* iv);
    const AP4_UI08* GetIv(AP4_Ordinal sample_index);
    AP4_Result      AddSubSampleData(AP4_Cardinal    subsample_count,
                                     const AP4_UI08* subsample_data);
    bool            HasSubSampleInfo() { 
        return m_SubSampleMapStarts.ItemCount() != 0; 
    }
    unsigned int    GetSubsampleCount(AP4_Cardinal sample_index) {
        if (sample_index < m_SampleCount) {
            return m_SubSampleMapLengths[sample_index];
        } else {
            return 0;
        }
    }
    AP4_Result GetSampleInfo(AP4_Cardinal     sample_index,
                             AP4_Cardinal&    subsample_count,
                             const AP4_UI16*& bytes_of_cleartext_data,
                             const AP4_UI32*& bytes_of_encrypted_data);
    
    AP4_Result GetSubsampleInfo(AP4_Cardinal sample_index, 
                                AP4_Cardinal subsample_index,
                                AP4_UI16&    bytes_of_cleartext_data,
                                AP4_UI32&    bytes_of_encrypted_data);
                                
    // see note below regarding the serialization format
    AP4_Result Serialize(AP4_DataBuffer& buffer);
    
private:
    AP4_UI32                m_SampleCount;
    AP4_UI08                m_IvSize;
    AP4_DataBuffer          m_IvData;
    AP4_Array<AP4_UI16>     m_BytesOfCleartextData;
    AP4_Array<AP4_UI32>     m_BytesOfEncryptedData;
    AP4_Array<unsigned int> m_SubSampleMapStarts;
    AP4_Array<unsigned int> m_SubSampleMapLengths;
};

/*----------------------------------------------------------------------
|   AP4_CencSampleInfoTable serialization format
|   
|   (All integers are stored in big-endian byte order)
|   
|   +---------------+----------------+------------------------------------+
|   | Size          |  Type          |  Description                       |
|   +---------------+----------------+------------------------------------+
|
|   +---------------+----------------+------------------------------------+
|   | 4 bytes       | 32-bit integer | sample_count                       |
|   +---------------+----------------+------------------------------------+
|   | 4 bytes       | 32-bit integer | iv_size                            |
|   +---------------+----------------+------------------------------------+
|
|   repeat sample_count times:
|   +---------------+----------------+------------------------------------+
|   | iv_size bytes | byte array     | IV[i]                              |
|   +---------------+----------------+------------------------------------+
|
|   +---------------+----------------+------------------------------------+
|   | 4 bytes       | 32-bit integer | entry_count                        |
|   +---------------+----------------+------------------------------------+
|
|   repeat entry_count times:
|   +---------------+----------------+------------------------------------+
|   | 2 bytes       | 16-bit integer | bytes_of_cleartext_data[i]         |
|   +---------------+----------------+------------------------------------+
|
|   repeat entry_count times:
|   +---------------+----------------+------------------------------------+
|   | 4 bytes       | 32-bit integer | bytes_of_encrypted_data[i]         |
|   +---------------+----------------+------------------------------------+
|
|   +---------------+----------------+------------------------------------+
|   | 4 bytes       | 32-bit flags   | 1 if subsamples are used, 0 if not |
|   +---------------+----------------+------------------------------------+
|
|   if subsamples are used, repeat sample_count times:
|   +---------------+----------------+------------------------------------+
|   | 4 bytes       | 32-bit integer | subsample_map_start[i]             |
|   +---------------+----------------+------------------------------------+
|
|   if subsamples are used, repeat sample_count times:
|   +---------------+----------------+------------------------------------+
|   | 4 bytes       | 32-bit integer | subsample_map_length[i]            |
|   +---------------+----------------+------------------------------------+
|
|   NOTES: subsample_map_start[i] ans subsample_map_length[i] are, respecitvely,
|   the index and the length the i'th subsample map sequence in the
|   bytes_of_cleartext_data anb bytes_of_encrypted_data arrays.
|   For example, if we have:
|   bytes_of_cleartext_data[] = { 10,   15, 13, 17, 12 }
|   bytes_of_encrypted_data[] = { 100, 200, 50, 80, 32 }
|   subsample_map_start  = { 0, 3 }
|   subsample_map_length = { 3, 2 }
|   It means that the (bytes_of_cleartext_data, bytes_of_encrypted_data)
|   sequences for the two subsamples are:
|   subsample[0] --> [(10,100), (15, 200), (13, 50)] 
|   subsample[1] --> [(17, 80), (12,  32)] 
|
+---------------------------------------------------------------------*/


/*----------------------------------------------------------------------
|   AP4_CencSampleEncrypter
+---------------------------------------------------------------------*/
class AP4_CencSampleEncrypter
{
public:
    // constructor and destructor
    AP4_CencSampleEncrypter(AP4_StreamCipher* cipher) : m_Cipher(cipher) { 
        AP4_SetMemory(m_Iv, 0, 16); 
    };
    virtual ~AP4_CencSampleEncrypter();

    // methods
    virtual AP4_Result EncryptSampleData(AP4_DataBuffer& data_in,
                                         AP4_DataBuffer& data_out, 
                                         AP4_DataBuffer& sample_infos) = 0;    

    void            SetIv(const AP4_UI08* iv) { AP4_CopyMemory(m_Iv, iv, 16); }
    const AP4_UI08* GetIv()                   { return m_Iv;                  }
    virtual bool    UseSubSamples()           { return false;                 }
    virtual AP4_Result GetSubSampleMap(AP4_DataBuffer&      /* sample_data */, 
                                       AP4_Array<AP4_UI16>& /* bytes_of_cleartext_data */, 
                                       AP4_Array<AP4_UI32>& /* bytes_of_encrypted_data */) { 
        return AP4_SUCCESS;
    }
    
protected:
    AP4_UI08          m_Iv[16];
    AP4_StreamCipher* m_Cipher;
};

/*----------------------------------------------------------------------
|   AP4_CencCtrSampleEncrypter
+---------------------------------------------------------------------*/
class AP4_CencCtrSampleEncrypter : public AP4_CencSampleEncrypter
{
public:
    // constructor and destructor
    AP4_CencCtrSampleEncrypter(AP4_StreamCipher* cipher, unsigned int iv_size) :
        AP4_CencSampleEncrypter(cipher),
        m_IvSize(iv_size) {}

    // methods
    virtual AP4_Result EncryptSampleData(AP4_DataBuffer& data_in,
                                         AP4_DataBuffer& data_out,
                                         AP4_DataBuffer& sample_infos);
    
protected:
    unsigned int m_IvSize;
};

/*----------------------------------------------------------------------
|   AP4_CencCbcSampleEncrypter
+---------------------------------------------------------------------*/
class AP4_CencCbcSampleEncrypter : public AP4_CencSampleEncrypter
{
public:
    // constructor and destructor
    AP4_CencCbcSampleEncrypter(AP4_StreamCipher* cipher) :
        AP4_CencSampleEncrypter(cipher) {}

    // methods
    virtual AP4_Result EncryptSampleData(AP4_DataBuffer& data_in,
                                         AP4_DataBuffer& data_out,
                                         AP4_DataBuffer& sample_infos);
};

/*----------------------------------------------------------------------
|   AP4_CencSubSampleEncrypter
+---------------------------------------------------------------------*/
class AP4_CencSubSampleEncrypter : public AP4_CencSampleEncrypter
{
public:
    // constructor and destructor
    AP4_CencSubSampleEncrypter(AP4_StreamCipher* cipher,
                               AP4_Size          nalu_length_size) :
        AP4_CencSampleEncrypter(cipher),
        m_NaluLengthSize(nalu_length_size) {}

    // methods
    virtual bool UseSubSamples() { return true;}
                                         
    // members
    AP4_Size m_NaluLengthSize;
};

/*----------------------------------------------------------------------
|   AP4_CencCtrSubSampleEncrypter
+---------------------------------------------------------------------*/
class AP4_CencCtrSubSampleEncrypter : public AP4_CencSubSampleEncrypter
{
public:
    // constructor and destructor
    AP4_CencCtrSubSampleEncrypter(AP4_StreamCipher* cipher,
                                  AP4_Size          nalu_length_size,
                                  unsigned int      iv_size) :
        AP4_CencSubSampleEncrypter(cipher, nalu_length_size),
        m_IvSize(iv_size) {}

    // methods
    virtual AP4_Result GetSubSampleMap(AP4_DataBuffer&      sample_data, 
                                       AP4_Array<AP4_UI16>& bytes_of_cleartext_data, 
                                       AP4_Array<AP4_UI32>& bytes_of_encrypted_data);
    virtual AP4_Result EncryptSampleData(AP4_DataBuffer& data_in,
                                         AP4_DataBuffer& data_out,
                                         AP4_DataBuffer& sample_infos);
    
protected:
    unsigned int m_IvSize;
};

/*----------------------------------------------------------------------
|   AP4_CencCbcSubSampleEncrypter
+---------------------------------------------------------------------*/
class AP4_CencCbcSubSampleEncrypter : public AP4_CencSubSampleEncrypter
{
public:
    // constructor and destructor
    AP4_CencCbcSubSampleEncrypter(AP4_StreamCipher* cipher,
                                  AP4_Size          nalu_length_size) :
        AP4_CencSubSampleEncrypter(cipher, nalu_length_size) {}

    // methods
    virtual AP4_Result GetSubSampleMap(AP4_DataBuffer&      sample_data, 
                                       AP4_Array<AP4_UI16>& bytes_of_cleartext_data, 
                                       AP4_Array<AP4_UI32>& bytes_of_encrypted_data);
    virtual AP4_Result EncryptSampleData(AP4_DataBuffer& data_in,
                                         AP4_DataBuffer& data_out,
                                         AP4_DataBuffer& sample_infos);
};

/*----------------------------------------------------------------------
|   AP4_CencEncryptingProcessor
+---------------------------------------------------------------------*/
class AP4_CencEncryptingProcessor : public AP4_Processor
{
public:
    // types
    struct Encrypter {
        Encrypter(AP4_UI32 track_id, AP4_CencSampleEncrypter* sample_encrypter) :
            m_TrackId(track_id),
            m_SampleEncrypter(sample_encrypter) {}
        ~Encrypter() { delete m_SampleEncrypter; }
        AP4_UI32                 m_TrackId;
        AP4_CencSampleEncrypter* m_SampleEncrypter;
    };

    // constructor
    AP4_CencEncryptingProcessor(AP4_CencVariant         variant,
                                AP4_BlockCipherFactory* block_cipher_factory = NULL);
    ~AP4_CencEncryptingProcessor();

    // accessors
    AP4_ProtectionKeyMap&     GetKeyMap()      { return m_KeyMap;      }
    AP4_TrackPropertyMap&     GetPropertyMap() { return m_PropertyMap; }
    AP4_Array<AP4_PsshAtom*>& GetPsshAtoms()   { return m_PsshAtoms;   }
    
    // AP4_Processor methods
    virtual AP4_Result Initialize(AP4_AtomParent&   top_level,
                                  AP4_ByteStream&   stream,
                                  AP4_Processor::ProgressListener* listener = NULL);
    virtual AP4_Processor::TrackHandler*    CreateTrackHandler(AP4_TrakAtom* trak);
    virtual AP4_Processor::FragmentHandler* CreateFragmentHandler(AP4_TrexAtom*      trex,
                                                                  AP4_ContainerAtom* traf,
                                                                  AP4_ByteStream&    moof_data,
                                                                  AP4_Position       moof_offset);
    
protected:    
    // members
    AP4_CencVariant          m_Variant;
    AP4_BlockCipherFactory*  m_BlockCipherFactory;
    AP4_ProtectionKeyMap     m_KeyMap;
    AP4_TrackPropertyMap     m_PropertyMap;
    AP4_Array<AP4_PsshAtom*> m_PsshAtoms;
    AP4_List<Encrypter>      m_Encrypters;
};

/*----------------------------------------------------------------------
|   AP4_CencDecryptingProcessor
+---------------------------------------------------------------------*/
class AP4_CencDecryptingProcessor : public AP4_Processor
{
public:
    // constructor
    AP4_CencDecryptingProcessor(const AP4_ProtectionKeyMap* key_map, 
                                AP4_BlockCipherFactory*     block_cipher_factory = NULL);

    // AP4_Processor methods
    virtual AP4_Processor::TrackHandler*    CreateTrackHandler(AP4_TrakAtom* trak);
    virtual AP4_Processor::FragmentHandler* CreateFragmentHandler(AP4_TrexAtom*      trex,
                                                                  AP4_ContainerAtom* traf,
                                                                  AP4_ByteStream&    moof_data,
                                                                  AP4_Position       moof_offset);
    
protected:    
    // members
    AP4_BlockCipherFactory*     m_BlockCipherFactory;
    const AP4_ProtectionKeyMap* m_KeyMap;
};

/*----------------------------------------------------------------------
|   AP4_CencSingleSampleDecrypter
+---------------------------------------------------------------------*/
class AP4_CencSingleSampleDecrypter
{
public:
    static AP4_Result Create(AP4_UI32                        algorithm_id,
                             const AP4_UI08*                 key,
                             AP4_Size                        key_size,
                             AP4_BlockCipherFactory*         block_cipher_factory,
                             AP4_CencSingleSampleDecrypter*& decrypter);
    
    // methods
    AP4_CencSingleSampleDecrypter(AP4_StreamCipher* cipher) : m_Cipher(cipher), m_FullBlocksOnly(false) {}
    virtual ~AP4_CencSingleSampleDecrypter();
    virtual AP4_Result DecryptSampleData(AP4_DataBuffer& data_in,
                                         AP4_DataBuffer& data_out,
                                         
                                         // always 16 bytes
                                         const AP4_UI08* iv,
                                         
                                         // pass 0 for full decryption
                                         unsigned int    subsample_count,
                                         
                                         // array of <subsample_count> integers. NULL if subsample_count is 0
                                         const AP4_UI16* bytes_of_cleartext_data,
                                         
                                         // array of <subsample_count> integers. NULL if subsample_count is 0
                                         const AP4_UI32* bytes_of_encrypted_data);  
                                             
private:
    // constructor
    AP4_CencSingleSampleDecrypter(AP4_StreamCipher* cipher, bool full_blocks_only) :
        m_Cipher(cipher),
        m_FullBlocksOnly(full_blocks_only) {}

    // members
    AP4_StreamCipher* m_Cipher;
    bool              m_FullBlocksOnly;
};

/*----------------------------------------------------------------------
|   AP4_CencSampleDecrypter
+---------------------------------------------------------------------*/
class AP4_CencSampleDecrypter : public AP4_SampleDecrypter
{
public:
    static AP4_Result Create(AP4_ProtectedSampleDescription* sample_description,
                             AP4_ContainerAtom*              traf,
                             AP4_ByteStream&                 moof_data,
                             AP4_Position                    moof_offset,
                             const AP4_UI08*                 key, 
                             AP4_Size                        key_size,
                             AP4_BlockCipherFactory*         block_cipher_factory,
                             AP4_SaioAtom*&                  saio_atom,              // [out]
                             AP4_SaizAtom*&                  saiz_atom,              // [out]
                             AP4_CencSampleEncryption*&      sample_encryption_atom, // [out]
                             AP4_CencSampleDecrypter*&       decrypter);

    static AP4_Result Create(AP4_ProtectedSampleDescription* sample_description, 
                             AP4_ContainerAtom*              traf,
                             AP4_ByteStream&                 moof_data,
                             AP4_Position                    moof_offset,
                             const AP4_UI08*                 key, 
                             AP4_Size                        key_size,
                             AP4_BlockCipherFactory*         block_cipher_factory,
                             AP4_CencSampleDecrypter*&       decrypter);

    static AP4_Result Create(AP4_CencSampleInfoTable*  sample_info_table,
                             AP4_UI32                  algorithm_id,
                             const AP4_UI08*           key, 
                             AP4_Size                  key_size,
                             AP4_BlockCipherFactory*   block_cipher_factory,
                             AP4_CencSampleDecrypter*& decrypter);
    
    // methods
    AP4_CencSampleDecrypter(AP4_CencSingleSampleDecrypter* single_sample_decrypter,
                            AP4_CencSampleInfoTable*       sample_info_table) :
        m_SingleSampleDecrypter(single_sample_decrypter),
        m_SampleInfoTable(sample_info_table),
        m_SampleCursor(0) {}
    virtual ~AP4_CencSampleDecrypter();
    virtual AP4_Result SetSampleIndex(AP4_Ordinal sample_index);
    virtual AP4_Result DecryptSampleData(AP4_DataBuffer& data_in,
                                         AP4_DataBuffer& data_out,
                                         const AP4_UI08* iv);
                                             
protected:
    AP4_CencSingleSampleDecrypter* m_SingleSampleDecrypter;
    AP4_CencSampleInfoTable*       m_SampleInfoTable;
    AP4_Ordinal                    m_SampleCursor;
};

/*----------------------------------------------------------------------
|   AP4_CencSampleEncryptionInformationGroupEntry
+---------------------------------------------------------------------*/
class AP4_CencSampleEncryptionInformationGroupEntry {
public:
    AP4_CencSampleEncryptionInformationGroupEntry(const AP4_UI08* data);
    
    // accessors
    bool            IsEncrypted() const { return m_IsEncrypted; }
    AP4_UI08        GetIvSize()   const { return m_IvSize;      }
    const AP4_UI08* GetKid()      const { return &m_KID[9];     }
    
private:
    // members
    bool     m_IsEncrypted;
    AP4_UI08 m_IvSize;
    AP4_UI08 m_KID[16];
};

#endif // _AP4_COMMON_ENCRYPTION_H_
