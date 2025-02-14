import requests

userServiceURL = "http://localhost:8080"
marketplaceServiceURL = "http://localhost:8081"
walletServiceURL = "http://localhost:8082"

def main():
    name = "John Doe"
    email = "johndoe@mail.com"
    productId = 7
    add_money_and_place_order(name, email, productId)

def create_user(name, email):
    new_user = {"name": name, "email": email}
    response = requests.post(userServiceURL + "/users", json=new_user)
    return response

def create_wallet(user_id):
    requests.put(walletServiceURL+f"/wallets/{user_id}", json={"action":"credit", "amount":0})

def get_wallet(user_id):
    response = requests.get(walletServiceURL + f"/wallets/{user_id}")
    return response

def update_wallet(user_id, action, amount):
    response = requests.put(walletServiceURL + f"/wallets/{user_id}", json={"action":action, "amount":amount})
    return response

def get_product_details(product_id):
    response = requests.get(marketplaceServiceURL + f"/proudcts/{product_id}")
    return response   


def delete_order(user_id):
    response = requests.delete(marketplaceServiceURL+f"/marketplace/users/{user_id}")
    return response

def delete_users():
    requests.delete(userServiceURL+f"/users")    

def add_money_and_place_order(name,email,productId):
    try:
        delete_users()
        new_user = create_user(name,email) #create_user
        new_userid = new_user.json()['id']
        update_wallet(new_userid,"credit",1000) #update_wallet
        product_details_before_ordering = get_product_details(productId) #get_product_details
        old_wallet_balance = get_wallet(new_userid).json()['balance'] #get_wallet
        new_order = {"items": [{"product_id": productId, "quantity": 2}], "user_id": new_userid}
        requests.post(marketplaceServiceURL + "/orders", json=new_order)
        delete_order(new_userid) #delete_order
        product_details_after_ordering = get_product_details(productId)
        # checking if the refund and stock quantity is correct after an undelivered order is cancelled
        if(product_details_after_ordering.json()['stock_quantity'] == product_details_before_ordering.json()['stock_quantity'] and old_wallet_balance == get_wallet(new_userid).json()['balance']):
            print("Test passed")
        else:
            print("Test failed")
    except:
        print("Some Exception Occurred")

if __name__ == "__main__":
    main()