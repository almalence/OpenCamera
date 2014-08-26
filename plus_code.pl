
$srcfn = shift;

if (!$srcfn)
{
	print "Usage:\nperl plus_code.pl <source_file_name>\n";
	exit();
}

$suppress = 0;

@AVS = `cat $srcfn`;
foreach (@AVS)
{
	$str="DEADBEEF";

	$orig = $_;

	chomp;
	tr/\015//d;

	if (($str)=/(.*)\<\!\-\-[\x00-\x20]\+\+\+[\x00-\x20]*/)
	{
	}
	elsif (($str)=/(.*)\+\+\+[\x00-\x20]\-\-\>[\x00-\x20]*/)
	{
	}
	elsif (($str)=/(.*)\<\!\-\-[\x00-\x20]\-\+\-[\x00-\x20]*/)
	{
		$suppress = 1;
	}
	elsif (($str)=/(.*)\-\+\-[\x00-\x20]\-\-\>[\x00-\x20]*/)
	{
		$suppress = 0;
	}
	else
	{
		if (!$suppress)
		{
			print "$orig";
		}
	}
}
