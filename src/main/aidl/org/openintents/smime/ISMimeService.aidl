// ISMimeService.aidl
package org.openintents.smime;

// Declare any non-default types here with import statements

interface ISMimeService {

    /**
     * see org.openintents.smime.util.SMimeApi for documentation
     */
    ParcelFileDescriptor createOutputPipe(in int pipeId);

    /**
     * see org.openintents.smime.util.SMimeApi for documentation
     */
    Intent execute(in Intent data, in ParcelFileDescriptor input, int pipeId);
}
