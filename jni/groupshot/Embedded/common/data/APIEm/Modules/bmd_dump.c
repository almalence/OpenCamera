#include <stdio.h>
#include <fcntl.h>
#include <stdlib.h>

int main(int ac, char *av[])
{
    FILE *f;
    int i, len;
    unsigned char c;
    
    if (ac!=2)
    {
	printf ("Usage: bmd_dump file.bmd\n");
	exit(-1);
    }
    
    f = fopen (av[1], "r");
    
    if (f==NULL)
    {
	printf ("Can't open file %s\n", av[1]);
	exit(-1);
    }
    
    fseek(f, 0, SEEK_END);
    len = ftell(f);
    fseek(f, 0, SEEK_SET);
    
    printf ("= {\n\t");
    for (i=0; i<len; ++i)
    {
	c = fgetc(f);
	printf ("0x%02X,", c);
	if ((i%20)==19)
	    printf ("\n\t");
    }
    printf ("0x00\n};\n");
    
    fclose(f);
    return 0;
}
