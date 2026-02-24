import { useState, useCallback, useRef } from 'react';
import { Sparkles, Upload, FileText, Image as ImageIcon, X, Loader2 } from 'lucide-react';
import { Button } from '@/components/ui/button';
import { Textarea } from '@/components/ui/textarea';
import { Tabs, TabsContent, TabsList, TabsTrigger } from '@/components/ui/tabs';
import { Alert, AlertDescription } from '@/components/ui/alert';
import { useChartStore } from '@/store/chartStore';
import { generateOrgChartFromText, generateOrgChartFromImage } from '@/services/gemini';

const AIInputPanel = () => {
  const [inputText, setInputText] = useState('');
  const [uploadedImage, setUploadedImage] = useState<string | null>(null);
  const [imageMimeType, setImageMimeType] = useState<string>('');
  const [isGenerating, setIsGenerating] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const fileInputRef = useRef<HTMLInputElement>(null);
  
  const setOrgData = useChartStore((state) => state.setOrgData);
  const setGenerating = useChartStore((state) => state.setGenerating);
  const storeSetError = useChartStore((state) => state.setError);

  const handleGenerateFromText = useCallback(async () => {
    if (!inputText.trim()) {
      setError('Please enter a description of your organization');
      return;
    }

    setIsGenerating(true);
    setGenerating(true);
    setError(null);
    storeSetError(null);

    try {
      const orgChart = await generateOrgChartFromText(inputText);
      setOrgData(orgChart);
    } catch (err) {
      const message = err instanceof Error ? err.message : 'Failed to generate org chart';
      setError(message);
      storeSetError(message);
    } finally {
      setIsGenerating(false);
      setGenerating(false);
    }
  }, [inputText, setOrgData, setGenerating, storeSetError]);

  const handleFileUpload = useCallback((event: React.ChangeEvent<HTMLInputElement>) => {
    const file = event.target.files?.[0];
    if (!file) return;

    // Validate file type
    if (!file.type.startsWith('image/')) {
      setError('Please upload an image file');
      return;
    }

    // Validate file size (max 5MB)
    if (file.size > 5 * 1024 * 1024) {
      setError('Image size should be less than 5MB');
      return;
    }

    const reader = new FileReader();
    reader.onload = (e) => {
      const result = e.target?.result as string;
      // Remove data URL prefix to get base64
      const base64 = result.split(',')[1];
      setUploadedImage(result);
      setImageMimeType(file.type);
      setUploadedImage(base64);
    };
    reader.readAsDataURL(file);
  }, []);

  const handleGenerateFromImage = useCallback(async () => {
    if (!uploadedImage) {
      setError('Please upload an image first');
      return;
    }

    setIsGenerating(true);
    setGenerating(true);
    setError(null);
    storeSetError(null);

    try {
      const orgChart = await generateOrgChartFromImage(uploadedImage, imageMimeType);
      setOrgData(orgChart);
    } catch (err) {
      const message = err instanceof Error ? err.message : 'Failed to parse org chart from image';
      setError(message);
      storeSetError(message);
    } finally {
      setIsGenerating(false);
      setGenerating(false);
    }
  }, [uploadedImage, imageMimeType, setOrgData, setGenerating, storeSetError]);

  const clearImage = useCallback(() => {
    setUploadedImage(null);
    setImageMimeType('');
    if (fileInputRef.current) {
      fileInputRef.current.value = '';
    }
  }, []);

  const examplePrompts = [
    'Our company has a CEO named John Smith. Under him are 3 VPs: Sarah (Engineering), Mike (Sales), and Lisa (Marketing). Sarah manages 5 engineers.',
    'Small startup: Founder/CEO Alex, CTO Jordan with 3 developers, CMO Taylor with 2 marketers.',
    'Fortune 500 structure: CEO, 4 EVPs (Operations, Finance, Technology, HR), each with 2-3 SVPs underneath.',
  ];

  return (
    <div className="h-full flex flex-col bg-white border-r border-slate-200">
      <div className="p-4 border-b border-slate-200">
        <h2 className="text-lg font-semibold text-slate-900 flex items-center gap-2">
          <Sparkles className="w-5 h-5 text-amber-500" />
          AI Org Chart Generator
        </h2>
        <p className="text-sm text-slate-500 mt-1">
          Describe your organization or upload an existing chart
        </p>
      </div>

      <Tabs defaultValue="text" className="flex-1 flex flex-col">
        <TabsList className="mx-4 mt-4 grid w-auto grid-cols-2">
          <TabsTrigger value="text" className="gap-2">
            <FileText className="w-4 h-4" />
            Text Input
          </TabsTrigger>
          <TabsTrigger value="image" className="gap-2">
            <ImageIcon className="w-4 h-4" />
            Upload Image
          </TabsTrigger>
        </TabsList>

        <TabsContent value="text" className="flex-1 flex flex-col m-0 mt-4 px-4">
          <div className="flex-1 flex flex-col gap-3">
            <Textarea
              placeholder="Describe your organization structure..."
              value={inputText}
              onChange={(e) => setInputText(e.target.value)}
              className="flex-1 min-h-[200px] resize-none"
              disabled={isGenerating}
            />

            <div className="space-y-2">
              <p className="text-xs text-slate-500 font-medium">Example prompts:</p>
              <div className="space-y-1">
                {examplePrompts.map((prompt, idx) => (
                  <button
                    key={idx}
                    onClick={() => setInputText(prompt)}
                    className="text-xs text-left text-slate-600 hover:text-blue-600 
                             hover:bg-blue-50 p-2 rounded w-full transition-colors"
                    disabled={isGenerating}
                  >
                    {prompt.length > 80 ? prompt.slice(0, 80) + '...' : prompt}
                  </button>
                ))}
              </div>
            </div>
          </div>

          <Button
            onClick={handleGenerateFromText}
            disabled={isGenerating || !inputText.trim()}
            className="mt-4 w-full gap-2"
            size="lg"
          >
            {isGenerating ? (
              <>
                <Loader2 className="w-4 h-4 animate-spin" />
                Generating...
              </>
            ) : (
              <>
                <Sparkles className="w-4 h-4" />
                Generate Org Chart
              </>
            )}
          </Button>
        </TabsContent>

        <TabsContent value="image" className="flex-1 flex flex-col m-0 mt-4 px-4">
          <div className="flex-1 flex flex-col gap-3">
            {!uploadedImage ? (
              <div
                onClick={() => fileInputRef.current?.click()}
                className="flex-1 border-2 border-dashed border-slate-300 rounded-lg 
                         flex flex-col items-center justify-center gap-3 p-6
                         hover:border-blue-400 hover:bg-blue-50/50 cursor-pointer transition-colors"
              >
                <div className="w-12 h-12 rounded-full bg-blue-100 flex items-center justify-center">
                  <Upload className="w-6 h-6 text-blue-600" />
                </div>
                <div className="text-center">
                  <p className="text-sm font-medium text-slate-700">
                    Click to upload or drag and drop
                  </p>
                  <p className="text-xs text-slate-500 mt-1">
                    PNG, JPG, or GIF (max 5MB)
                  </p>
                </div>
                <input
                  ref={fileInputRef}
                  type="file"
                  accept="image/*"
                  onChange={handleFileUpload}
                  className="hidden"
                />
              </div>
            ) : (
              <div className="flex-1 flex flex-col gap-3">
                <div className="relative flex-1 border rounded-lg overflow-hidden bg-slate-50">
                  <img
                    src={`data:${imageMimeType};base64,${uploadedImage}`}
                    alt="Uploaded org chart"
                    className="w-full h-full object-contain"
                  />
                  <button
                    onClick={clearImage}
                    className="absolute top-2 right-2 w-8 h-8 rounded-full bg-white shadow-md
                             flex items-center justify-center hover:bg-red-50 hover:text-red-600
                             transition-colors"
                  >
                    <X className="w-4 h-4" />
                  </button>
                </div>
                <Button
                  onClick={handleGenerateFromImage}
                  disabled={isGenerating}
                  className="w-full gap-2"
                  size="lg"
                >
                  {isGenerating ? (
                    <>
                      <Loader2 className="w-4 h-4 animate-spin" />
                      Analyzing Image...
                    </>
                  ) : (
                    <>
                      <Sparkles className="w-4 h-4" />
                      Extract Org Chart
                    </>
                  )}
                </Button>
              </div>
            )}
          </div>
        </TabsContent>
      </Tabs>

      {error && (
        <div className="p-4">
          <Alert variant="destructive">
            <AlertDescription>{error}</AlertDescription>
          </Alert>
        </div>
      )}
    </div>
  );
};

export default AIInputPanel;
