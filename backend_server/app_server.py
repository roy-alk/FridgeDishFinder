import google.generativeai as genai
from PIL import Image
from flask import Flask, request, jsonify
import io
import os
from dotenv import load_dotenv

load_dotenv()

genai.configure(api_key= os.getenv('API_KEY'))

model = genai.GenerativeModel('gemini-1.5-pro')

app = Flask(__name__)

@app.route('/upload', methods=['POST'])

def upload_image():
    try:

        # Access the file from the request
        file = request.files['image']
        
        # Convert the uploaded file to a PIL Image
        fridge_image = Image.open(file.stream)
        
        prompt = "Check if this picture shows an opened fridge. If it does, return exactly 'true'. If not, return exactly 'false'."
        response = model.generate_content([fridge_image, prompt])
        
        if response.text.strip() == 'true':
            # If it's an opened fridge, get dish suggestions
            
            dishes_prompt = """
            Thoroughly scan the opened fridge to figure out the ingredients and suggest dishes that can be made using ONLY the available ingredients. Follow these strict guidelines:

            1. Start with EXACTLY ONE brief header line ending with a colon, such as "Based on your fridge contents, here are some dish suggestions:" or "Dishes you can make with available ingredients:". Do not include any other introductory text.

            2. After the header, immediately list the dishes under their respective categories.

            3. For each suggested dish, provide a detailed recipe.

            4. Do not include cooking instructions or list the ingredients in the fridge.

            5. Only suggest dishes that can be made with ingredients visible in the fridge.

            6. Do not ever repeat a dish in different multiple categories.

            7. End with a single closing remark.

            8. Use these categories, ending each with a colon, and only include categories with dishes:
            Breakfast:
            Lunch:
            Dinner:
            Snacks:
            Vegan:
            Soups:
            Keto:
            Low Calorie:
            Beverages & Smoothies:
            Desserts:

            9. Format each dish as "Dish Name: Brief recipe"

            10. Do not include any additional headers or explanations between categories.
            """
            
            dishes_response = model.generate_content([fridge_image, dishes_prompt])
            
            cleaned_response = dishes_response.text.replace('#', '').replace('-', '').replace('*', '').strip()

            return jsonify({
                 'dishes': cleaned_response
            })
        else:
            return jsonify({'error': 'Please photograph an opened fridge.'}), 400
    
    except Exception as e:
        return jsonify({'error': f'Error processing image: {str(e)}'}), 500
    
if __name__ == '__main__':
    app.run(debug=True, host='0.0.0.0')
